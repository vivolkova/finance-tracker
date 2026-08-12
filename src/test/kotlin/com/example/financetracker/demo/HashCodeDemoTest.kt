package demo.financetracker

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Демо equals/hashCode и бакетов на "сущности" с равенством по id
 * и КОНСТАНТНЫМ hashCode (javaClass.hashCode()) — как в JPA-сущностях.
 *
 * Бакет — ячейка внутри HashMap/HashSet. Кладём: hashCode -> номер бакета.
 * Ищем: hashCode -> бакет -> внутри перебор equals.
 */
class HashCodeDemoTest {

    // Мини-сущность: равенство по id, hashCode константный для всех экземпляров.
    private class Category(val id: Long, val name: String) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Category) return false
            return id != 0L && id == other.id          // равенство ТОЛЬКО по id
        }
        override fun hashCode(): Int = javaClass.hashCode()   // одинаков для всех Category
    }

    @Test
    fun `hashCode is the same for all instances of the class`() {
        assertEquals(
            Category(1, "a").hashCode(),
            Category(2, "b").hashCode(),
            "javaClass.hashCode() — константа: у всех Category один бакет"
        )
    }

    @Test
    fun `retrieve from HashMap by a key with the same id`() {
        val balances = hashMapOf(
            Category(1, "Аренда") to 5000,
            Category(2, "Еда") to 3000,
            Category(3, "Зарплата") to 90000
        )

        // достаём НОВЫМ объектом с тем же id — имя неважно (равенство по id)
        assertEquals(5000, balances[Category(1, "любое имя")])
        assertEquals(3000, balances[Category(2, "Продукты")])
        assertNull(balances[Category(99, "нет такой")])
    }

    @Test
    fun `HashSet finds an equal element by id and rejects duplicates`() {
        val set = hashSetOf(Category(1, "Аренда"), Category(2, "Еда"))

        assertTrue(Category(1, "неважно") in set)          // нашёлся по id=1
        assertFalse(Category(99, "нет") in set)
        assertFalse(set.add(Category(1, "другое имя")), "дубликат по id не добавится")
        assertEquals(2, set.size)
    }

    // ── Почему hashCode КОНСТАНТНЫЙ: id появляется после "save" ──
    private class Entity(var id: Long = 0) {
        override fun equals(other: Any?) = other is Entity && id != 0L && id == other.id
        override fun hashCode(): Int = javaClass.hashCode()   // не меняется при смене id
    }

    @Test
    fun `constant hashCode keeps the object findable after id changes`() {
        val e = Entity()               // id = 0 (не сохранён)
        val set = hashSetOf(e)
        e.id = 42                      // "БД присвоила id после save"

        assertTrue(e in set, "объект не потерялся: hashCode константный, бакет тот же")
    }

    // ── Контраст: hashCode ПО id -> объект теряется после смены id ──
    private class BadEntity(var id: Long = 0) {
        override fun equals(other: Any?) = other is BadEntity && id == other.id
        override fun hashCode(): Int = id.hashCode()          // ПЛОХО: зависит от изменяемого id
    }

    @Test
    fun `id-based hashCode loses the object after id changes`() {
        val e = BadEntity(id = 0)
        val set = hashSetOf(e)         // положили в бакет для hash(0)
        e.id = 42                      // хеш стал hash(42) -> ДРУГОЙ бакет

        assertFalse(
            e in set,
            "объект потерялся: contains ищет в бакете для 42, а он лежит в бакете для 0"
        )
    }
}
