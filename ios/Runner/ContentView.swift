import FamilyControls
import SwiftUI

struct ContentView: View {
    @State private var isDiscouragedPresented = true
    @State private var isEncouragedPresented = false

    @EnvironmentObject var model: MyModel

    @ViewBuilder
    func contentView() -> some View {
        switch globalMethodCall {
        case "selectAppsToDiscourage":
            FamilyActivityPicker(selection: $model.selectionToDiscourage).onChange(of: model.selectionToDiscourage) { _ in
                model.setShieldRestrictions()
            }

        case "selectAppsToEncourage":
            FamilyActivityPicker(selection: $model.selectionToEncourage)
                .onChange(of: model.selectionToEncourage) { _ in
                    MySchedule.setSchedule()
                }

        default:
            Text("Default")
            // Add the views you want to display for the default case
        }
    }

    var body: some View {
        VStack {
            contentView()
        }
    }
}

struct ContentView_Previews: PreviewProvider {
    static var previews: some View {
        ContentView()
            .environmentObject(MyModel())
    }
}
