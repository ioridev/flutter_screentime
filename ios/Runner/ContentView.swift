import SwiftUI
import FamilyControls

struct ContentView: View {
    @State private var isDiscouragedPresented = false
    @State private var isEncouragedPresented = false
    
    @EnvironmentObject var model: MyModel
    
    var body: some View {
        // Show the family activity picker and allow the guardian to chose from a list of apps, websites, and categories used by the family (currently limited to 50)
        // Once the guardian has made their selection, Homework can use opaque tokens returned by the picker to set restrictions on apps, websites, and categories each token represents.
        // Here I've added the familyActivityPicker view modifier to a Button in the app
        // and I've bound the picker's selection parameter to a property in the app's model
        // This will update my model whenever the guardian's selection is updated in the UI
        
        // QUESTION: How do I only show this to the guardian and not the child?
        VStack {
            Button("Select Apps to Discourage") {
                isDiscouragedPresented = true
            }
            .familyActivityPicker(isPresented: $isDiscouragedPresented, selection: $model.selectionToDiscourage)
            .onChange(of: model.selectionToDiscourage) { newSelection in
                model.setShieldRestrictions()
            }
            
            Button("Select Apps to monitor") {
                isEncouragedPresented = true
            }
            .familyActivityPicker(isPresented: $isEncouragedPresented, selection: $model.selectionToEncourage)
            .onChange(of: model.selectionToEncourage) { newSelection in
                MySchedule.setSchedule()
            }
            
            Button("monitor all apps", action: {
                model.selectionToEncourage = FamilyActivitySelection()
                MySchedule.setSchedule()
            })
            
            Button("Stop monitor", action: {
                MySchedule.unsetSchedule()
            })
        }

    }
}

struct ContentView_Previews: PreviewProvider {
    static var previews: some View {
        ContentView()
            .environmentObject(MyModel())
    }
}
