import { useState, useCallback } from 'react'

const STORAGE_KEY = 'snip_url_history'
const MAX_ITEMS = 50

export function useUrlHistory() {
  const [urls, setUrls] = useState(() => {
    try {
      return JSON.parse(localStorage.getItem(STORAGE_KEY) || '[]')
    } catch {
      return []
    }
  })

  const addUrl = useCallback((entry) => {
    setUrls((prev) => {
      // If this short code already exists, replace it — no duplicates
      const updated = [
        entry,
        ...prev.filter((u) => u.shortCode !== entry.shortCode),
      ].slice(0, MAX_ITEMS)

      localStorage.setItem(STORAGE_KEY, JSON.stringify(updated))
      return updated
    })
  }, [])

  const removeUrl = useCallback((shortCode) => {
    setUrls((prev) => {
      const updated = prev.filter((u) => u.shortCode !== shortCode)
      localStorage.setItem(STORAGE_KEY, JSON.stringify(updated))
      return updated
    })
  }, [])

  return { urls, addUrl, removeUrl }
}
