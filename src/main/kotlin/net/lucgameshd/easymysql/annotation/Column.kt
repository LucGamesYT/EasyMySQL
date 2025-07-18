package net.lucgameshd.easymysql.annotation

@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
annotation class Column(val name: String = "", val length: Int = 255)
