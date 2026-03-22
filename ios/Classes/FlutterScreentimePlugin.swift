import FamilyControls
import Flutter
import ManagedSettings
import SwiftUI
import UIKit

private enum StorageKey {
  static let sharedContainerId = "flutter_screentime.sharedContainerId"
  static let blockScreenConfig = "flutter_screentime.blockScreenConfig"
  static let blockedPackages = "flutter_screentime.blockedPackages"
  static let blockedSelection = "flutter_screentime.blockedSelection"
  static let blockingEnabled = "flutter_screentime.blockingEnabled"
}

private final class BlockedAppsPickerModel: ObservableObject {
  @Published var selection: FamilyActivitySelection

  init(selection: FamilyActivitySelection) {
    self.selection = selection
  }
}

@available(iOS 16.0, *)
private struct BlockedAppsPickerView: View {
  @ObservedObject var model: BlockedAppsPickerModel
  let onCancel: () -> Void
  let onSave: (FamilyActivitySelection) -> Void

  var body: some View {
    NavigationStack {
      FamilyActivityPicker(selection: $model.selection)
        .navigationTitle("Select Apps")
        .toolbar {
          ToolbarItem(placement: .cancellationAction) {
            Button("Cancel", action: onCancel)
          }
          ToolbarItem(placement: .confirmationAction) {
            Button("Done") {
              onSave(model.selection)
            }
          }
        }
    }
  }
}

public final class FlutterScreentimePlugin: NSObject, FlutterPlugin {
  private let store = ManagedSettingsStore()

  public static func register(with registrar: FlutterPluginRegistrar) {
    let channel = FlutterMethodChannel(
      name: "flutter_screentime",
      binaryMessenger: registrar.messenger()
    )
    let instance = FlutterScreentimePlugin()
    registrar.addMethodCallDelegate(instance, channel: channel)
  }

  public func handle(_ call: FlutterMethodCall, result: @escaping FlutterResult) {
    switch call.method {
    case "checkAuthorization":
      result(currentAuthorizationStatus())
    case "requestAuthorization":
      requestAuthorization(result: result)
    case "setSharedContainerId":
      guard let appGroupId = call.arguments as? String, !appGroupId.isEmpty else {
        result(FlutterError(code: "invalid_args", message: "Expected a non-empty app group id.", details: nil))
        return
      }
      UserDefaults.standard.set(appGroupId, forKey: StorageKey.sharedContainerId)
      synchronizeStoredValuesToSharedDefaults()
      result(nil)
    case "configureBlockScreen":
      guard let arguments = call.arguments as? [String: Any] else {
        result(FlutterError(code: "invalid_args", message: "Expected a configuration map.", details: nil))
        return
      }
      persist(arguments, forKey: StorageKey.blockScreenConfig)
      result(nil)
    case "setBlockedPackages":
      guard let blockedPackages = call.arguments as? [String] else {
        result(FlutterError(code: "invalid_args", message: "Expected a list of package names.", details: nil))
        return
      }
      persist(blockedPackages, forKey: StorageKey.blockedPackages)
      result(nil)
    case "selectBlockedApps":
      presentBlockedAppsPicker(result: result)
    case "startBlocking":
      applyStoredSelection(result: result)
    case "stopBlocking":
      persist(false, forKey: StorageKey.blockingEnabled)
      store.clearAllSettings()
      result(nil)
    default:
      result(FlutterMethodNotImplemented)
    }
  }

  private func currentAuthorizationStatus() -> String {
    switch AuthorizationCenter.shared.authorizationStatus {
    case .approved:
      return "approved"
    case .denied:
      return "denied"
    case .notDetermined:
      return "notDetermined"
    @unknown default:
      return "notDetermined"
    }
  }

  private func requestAuthorization(result: @escaping FlutterResult) {
    Task { @MainActor in
      do {
        try await AuthorizationCenter.shared.requestAuthorization(for: .individual)
        result(self.currentAuthorizationStatus())
      } catch {
        result(FlutterError(
          code: "authorization_failed",
          message: error.localizedDescription,
          details: nil
        ))
      }
    }
  }

  private func presentBlockedAppsPicker(result: @escaping FlutterResult) {
    guard #available(iOS 16.0, *) else {
      result(FlutterError(code: "unsupported_ios", message: "FamilyControls requires iOS 16 or later.", details: nil))
      return
    }

    guard let presenter = topViewController() else {
      result(FlutterError(code: "no_presenter", message: "No presenter was available for the FamilyActivityPicker.", details: nil))
      return
    }

    let model = BlockedAppsPickerModel(selection: storedSelection())
    let hostingController = UIHostingController(
      rootView: BlockedAppsPickerView(
        model: model,
        onCancel: { [weak presenter] in
          presenter?.dismiss(animated: true)
          result(FlutterError(code: "cancelled", message: "Selection was cancelled.", details: nil))
        },
        onSave: { [weak self, weak presenter] selection in
          guard let self else { return }
          presenter?.dismiss(animated: true)
          self.persist(selectionData(from: selection), forKey: StorageKey.blockedSelection)
          result(self.selectionSummary(selection))
        }
      )
    )
    hostingController.modalPresentationStyle = .fullScreen
    hostingController.isModalInPresentation = true
    presenter.present(hostingController, animated: true)
  }

  private func applyStoredSelection(result: FlutterResult) {
    let selection = storedSelection()
    let hasSelection = !selection.applicationTokens.isEmpty || !selection.categoryTokens.isEmpty
    guard hasSelection else {
      result(FlutterError(
        code: "missing_selection",
        message: "Call selectBlockedApps before startBlocking on iOS.",
        details: nil
      ))
      return
    }

    persist(true, forKey: StorageKey.blockingEnabled)
    store.shield.applications = selection.applicationTokens.isEmpty ? nil : selection.applicationTokens
    store.shield.applicationCategories = selection.categoryTokens.isEmpty
      ? nil
      : ShieldSettings.ActivityCategoryPolicy.specific(selection.categoryTokens)
    result(nil)
  }

  private func storedSelection() -> FamilyActivitySelection {
    guard
      let data = storedValue(forKey: StorageKey.blockedSelection) as? Data,
      let selection = try? PropertyListDecoder().decode(FamilyActivitySelection.self, from: data)
    else {
      return FamilyActivitySelection()
    }
    return selection
  }

  private func selectionData(from selection: FamilyActivitySelection) -> Data? {
    try? PropertyListEncoder().encode(selection)
  }

  private func selectionSummary(_ selection: FamilyActivitySelection) -> [String: Int] {
    [
      "applicationCount": selection.applicationTokens.count,
      "categoryCount": selection.categoryTokens.count,
    ]
  }

  private func persist(_ value: Any?, forKey key: String) {
    if let value {
      UserDefaults.standard.set(value, forKey: key)
      sharedDefaults()?.set(value, forKey: key)
    } else {
      UserDefaults.standard.removeObject(forKey: key)
      sharedDefaults()?.removeObject(forKey: key)
    }
  }

  private func storedValue(forKey key: String) -> Any? {
    sharedDefaults()?.object(forKey: key) ?? UserDefaults.standard.object(forKey: key)
  }

  private func sharedDefaults() -> UserDefaults? {
    guard let appGroupId = UserDefaults.standard.string(forKey: StorageKey.sharedContainerId) else {
      return nil
    }
    return UserDefaults(suiteName: appGroupId)
  }

  private func synchronizeStoredValuesToSharedDefaults() {
    guard let sharedDefaults = sharedDefaults() else {
      return
    }

    [
      StorageKey.blockScreenConfig,
      StorageKey.blockedPackages,
      StorageKey.blockedSelection,
      StorageKey.blockingEnabled,
    ].forEach { key in
      if let value = UserDefaults.standard.object(forKey: key) {
        sharedDefaults.set(value, forKey: key)
      }
    }
  }

  private func topViewController(base: UIViewController? = nil) -> UIViewController? {
    let baseController = base ?? UIApplication.shared
      .connectedScenes
      .compactMap { $0 as? UIWindowScene }
      .flatMap(\.windows)
      .first(where: \.isKeyWindow)?
      .rootViewController

    if let navigationController = baseController as? UINavigationController {
      return topViewController(base: navigationController.visibleViewController)
    }

    if let tabBarController = baseController as? UITabBarController,
       let selectedViewController = tabBarController.selectedViewController {
      return topViewController(base: selectedViewController)
    }

    if let presentedViewController = baseController?.presentedViewController {
      return topViewController(base: presentedViewController)
    }

    return baseController
  }
}
