// "Insert lacking comma(s) / semicolon(s)" "true"

enum class MyEnum {
    FIRST, SECOND<caret>
    /* The last one*/
    fun foo() = 1
}