package net.lucgameshd.easymysql.repository

interface SQLRepository<T> {
    fun findAll(): List<T>
    fun save(obj: T)
    fun delete(obj: T)
}
