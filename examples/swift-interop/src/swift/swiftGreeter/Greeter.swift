import Foundation

@objcMembers public class Greeter: NSObject {
    @objc(greetWithName:)
    public static func greet(name: String) -> String {
        return "Hello \(name), from Swift!"
    }
}
