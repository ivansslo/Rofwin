package com.winlator

data class VirtualControlElement(
    val name: String,
    val keyMapping: String,
    val relativeX: Float, // percentage of screen width (e.g. 10 for 10%)
    val relativeY: Float, // percentage of screen height
    val sizeDp: Int = 56
)

data class InputControlsProfile(
    val id: String,
    val name: String,
    val isDefault: Boolean = false,
    val controls: List<VirtualControlElement> = emptyList()
)

object InputProfileDefaults {
    val PRELOADED_PROFILES = listOf(
        InputControlsProfile(
            id = "p1",
            name = "GTA 5 Optimized Layout",
            isDefault = true,
            controls = listOf(
                VirtualControlElement("D-Pad (Move)", "W, A, S, D", 15f, 75f, 96),
                VirtualControlElement("Sprint (A)", "Space", 85f, 75f),
                VirtualControlElement("Jump (X)", "V", 85f, 55f),
                VirtualControlElement("Enter Car (Y)", "F", 75f, 65f),
                VirtualControlElement("Attack (RT)", "Mouse Left Click", 90f, 35f, 64),
                VirtualControlElement("Aim (LT)", "Mouse Right Click", 10f, 35f, 64),
                VirtualControlElement("Menu (Esc)", "Escape", 90f, 10f)
            )
        ),
        InputControlsProfile(
            id = "p2",
            name = "Skyrim Classic Layout",
            controls = listOf(
                VirtualControlElement("D-Pad (Move)", "W, A, S, D", 15f, 75f, 96),
                VirtualControlElement("Interact (E)", "E", 85f, 75f),
                VirtualControlElement("Shout (Z)", "Z", 85f, 55f),
                VirtualControlElement("Inventory (I)", "I", 75f, 65f),
                VirtualControlElement("Attack (Left)", "Mouse Left Click", 90f, 35f, 64),
                VirtualControlElement("Block (Right)", "Mouse Right Click", 10f, 35f, 64),
                VirtualControlElement("Menu", "Tab", 90f, 10f)
            )
        ),
        InputControlsProfile(
            id = "p3",
            name = "FlatOut 2 Racer Layout",
            controls = listOf(
                VirtualControlElement("Steer Left", "Left Arrow", 10f, 75f),
                VirtualControlElement("Steer Right", "Right Arrow", 25f, 75f),
                VirtualControlElement("Gas (Up)", "Up Arrow", 90f, 70f, 72),
                VirtualControlElement("Brake (Down)", "Down Arrow", 75f, 75f),
                VirtualControlElement("Nitro (Ctrl)", "Left Ctrl", 90f, 45f),
                VirtualControlElement("Reset (R)", "R", 90f, 15f)
            )
        ),
        InputControlsProfile(
            id = "p4",
            name = "Fallout 3 Vault Layout",
            controls = listOf(
                VirtualControlElement("D-Pad (Move)", "W, A, S, D", 15f, 75f, 96),
                VirtualControlElement("Activate", "E", 85f, 75f),
                VirtualControlElement("VATS", "V", 85f, 55f),
                VirtualControlElement("Pip-Boy", "Tab", 75f, 65f),
                VirtualControlElement("Shoot", "Mouse Left Click", 90f, 35f, 64)
            )
        ),
        InputControlsProfile(
            id = "p5",
            name = "General Keyboard/Mouse Emulation",
            controls = listOf(
                VirtualControlElement("Left Click", "Mouse Left", 85f, 75f),
                VirtualControlElement("Right Click", "Mouse Right", 70f, 75f),
                VirtualControlElement("Enter", "Enter", 85f, 55f),
                VirtualControlElement("Esc", "Escape", 10f, 15f)
            )
        )
    )
}
