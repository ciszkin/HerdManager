package by.ciszkin.herdmanager.domain.model

/**
 * Sort order for the model registry.
 *
 * Mirrors the sort options the ollama.com/search page offers; the data layer
 * maps each value to the corresponding `o` query parameter (POPULAR is also
 * the site's default ordering).
 */
enum class RegistrySort {
    POPULAR,
    NEWEST
}