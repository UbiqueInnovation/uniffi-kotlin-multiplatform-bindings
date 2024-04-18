/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

import SwiftUI
import ExamplesAppKotlin

struct ContentView: View {
    init(todoList: TodoList) {
        self.todoList = todoList
    }

    var todoList: TodoList
    let stride: UInt64 = UInt64(1)
    @State private var clicked = UInt64.zero
    @State private var text = ""
    var body: some View {
        VStack {
            HStack {
                Text("\(clicked)").padding()
                TextField("New task", text: $text, onCommit:  {
                    try! self.todoList.addEntry(entry: TodoEntry(text: "\(clicked) \(text)"))
                    text = ""
                    clicked = try! Uniffi_kmm_example_arithmetic_procmacro_commonKt.add(a: clicked, b: stride)
                }).padding()
            }

            List{
                Section(header: Text("Still To Do")) {
                    ForEach(todoList.getEntries(), id: \.self) { entry in
                        Text(entry.text)
                    }
                    .onDelete { index in
                        if let i = index.first {
                            let entry = todoList.getEntries()[i]
                            try! todoList.clearItem(todo: entry.text)
                            clicked = try! Uniffi_kmm_example_arithmetic_procmacro_commonKt.sub(a: clicked, b: stride)
                        }
                    }
                }
            }
        }
    }
}

#Preview {
    ContentView(todoList: TodoList())
}
