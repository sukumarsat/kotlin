// "Replace with 'Companion::class.java'" "true"
// WITH_RUNTIME
fun main() {
    val c: Class<Int.Companion> = Int.javaClass<caret>
}