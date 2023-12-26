package com.hexagonkt.realworld.domain

import com.hexagonkt.realworld.domain.model.User
import com.hexagonkt.store.Store

data class UsersService(
    val users: Store<User, String>
) {
    fun register(user: User): String =
        users.insertOne(user)

    fun login(email: String, password: String): User? {
        val filter = mapOf(User::email.name to email)
        val user = users.findOne(filter) ?: error("Not found")
        return if (user.password == password) user else null
    }

    fun deleteUser(username: String): Boolean =
        users.deleteOne(username)

    fun replaceUser(subject: String, updates: Map<String, *>): Boolean =
        users.updateOne(subject, updates)

    fun searchUser(subject: String): User? =
        users.findOne(subject)
}
