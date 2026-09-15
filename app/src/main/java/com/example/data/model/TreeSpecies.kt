package com.example.data.model

import androidx.compose.ui.graphics.Color

enum class TreeSpecies(
    val id: String,
    val displayName: String,
    val description: String,
    val requiredLevel: Int,
    val primaryColor: Long,
    val secondaryColor: Long,
    val foliageColor: Long,
    val xpMultiplier: Float
) {
    SEEDLING_OF_CLARITY(
        id = "clarity_seedling",
        displayName = "Seedling of Clarity",
        description = "A humble sprout nourished by initial thoughts and brainstorming.",
        requiredLevel = 1,
        primaryColor = 0xFF10B981,
        secondaryColor = 0xFF059669,
        foliageColor = 0xFF34D399,
        xpMultiplier = 1.0f
    ),
    SCHOLARS_ANCIENT_OAK(
        id = "scholars_oak",
        displayName = "Scholar's Ancient Oak",
        description = "Grown from deep literary research and relentless citation mining.",
        requiredLevel = 2,
        primaryColor = 0xFF059669,
        secondaryColor = 0xFF047857,
        foliageColor = 0xFF10B981,
        xpMultiplier = 1.1f
    ),
    AETHER_RUNIC_WILLOW(
        id = "aether_willow",
        displayName = "Aether Runic Willow",
        description = "A mystical weeping tree whose branches glow with mathematical proofs.",
        requiredLevel = 4,
        primaryColor = 0xFF8B5CF6,
        secondaryColor = 0xFF6366F1,
        foliageColor = 0xFFA78BFA,
        xpMultiplier = 1.25f
    ),
    GOLDEN_BODHI_OF_THESIS(
        id = "golden_bodhi",
        displayName = "Golden Bodhi of Thesis",
        description = "The tree of profound academic breakthrough and chapter completion.",
        requiredLevel = 7,
        primaryColor = 0xFFF59E0B,
        secondaryColor = 0xFFD97706,
        foliageColor = 0xFFFCD34D,
        xpMultiplier = 1.4f
    ),
    COSMIC_WORLD_TREE(
        id = "world_tree",
        displayName = "Chronicle World-Tree",
        description = "A colossal celestial tree woven from a defended PhD dissertation.",
        requiredLevel = 10,
        primaryColor = 0xFF06B6D4,
        secondaryColor = 0xFF3B82F6,
        foliageColor = 0xFF67E8F9,
        xpMultiplier = 1.6f
    );

    fun getColorPrimary(): Color = Color(primaryColor)
    fun getColorSecondary(): Color = Color(secondaryColor)
    fun getCalorFoliage(): Color = Color(foliageColor)

    companion object {
        fun fromId(id: String): TreeSpecies = entries.find { it.id == id } ?: SEEDLING_OF_CLARITY
    }
}

enum class ThesisTaskTag(
    val id: String,
    val label: String,
    val iconName: String,
    val colorLong: Long
) {
    LITERATURE_REVIEW(
        id = "lit_review",
        label = "Literature Review",
        iconName = "menu_book",
        colorLong = 0xFF3B82F6
    ),
    METHODOLOGY(
        id = "methodology",
        label = "Methodology & Experiments",
        iconName = "science",
        colorLong = 0xFF10B981
    ),
    CHAPTER_WRITING(
        id = "chapter_writing",
        label = "Chapter Writing",
        iconName = "edit_note",
        colorLong = 0xFFF59E0B
    ),
    DATA_ANALYSIS(
        id = "data_analysis",
        label = "Data Analysis",
        iconName = "analytics",
        colorLong = 0xFF8B5CF6
    ),
    CITATIONS_REFS(
        id = "citations",
        label = "Citations & References",
        iconName = "format_quote",
        colorLong = 0xFFEC4899
    ),
    DEFENSE_PREP(
        id = "defense_prep",
        label = "Defense Preparation",
        iconName = "school",
        colorLong = 0xFF06B6D4
    ),
    PROOFREADING(
        id = "proofreading",
        label = "Proofreading & Polish",
        iconName = "spellcheck",
        colorLong = 0xFF14B8A6
    );

    fun getColor(): Color = Color(colorLong)

    companion object {
        fun fromId(id: String): ThesisTaskTag = entries.find { it.id == id } ?: CHAPTER_WRITING
    }
}
