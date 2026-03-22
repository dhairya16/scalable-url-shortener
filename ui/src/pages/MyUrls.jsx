import React, { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import {
  Link2,
  Copy,
  Check,
  Trash2,
  ExternalLink,
  Search,
  Clock,
} from 'lucide-react'
import { deactivateUrl } from '../lib/api'
import { useUrlHistory } from '../hooks/useUrlHistory'
import { Button, Card, Empty, Toast } from '../components/ui'

function timeAgo(dateStr) {
  const diff = Date.now() - new Date(dateStr).getTime()
  const mins = Math.floor(diff / 60000)
  const hours = Math.floor(diff / 3600000)
  const days = Math.floor(diff / 86400000)
  if (mins < 1) return 'just now'
  if (mins < 60) return `${mins}m ago`
  if (hours < 24) return `${hours}h ago`
  return `${days}d ago`
}

function formatDate(dateStr) {
  if (!dateStr) return '—'
  return new Date(dateStr).toLocaleDateString('en-US', {
    month: 'short',
    day: 'numeric',
    year: 'numeric',
  })
}

export default function MyUrls() {
  const navigate = useNavigate()
  const { urls, removeUrl } = useUrlHistory()

  const [search, setSearch] = useState('')
  const [copiedId, setCopiedId] = useState(null)
  const [deletingId, setDeletingId] = useState(null)
  const [confirmDelete, setConfirmDelete] = useState(null)
  const [toast, setToast] = useState(null)

  const filtered = urls.filter(
    (u) =>
      u.shortCode?.toLowerCase().includes(search.toLowerCase()) ||
      u.longUrl?.toLowerCase().includes(search.toLowerCase()),
  )

  const copy = async (shortUrl, shortCode) => {
    await navigator.clipboard.writeText(shortUrl)
    setCopiedId(shortCode)
    setToast({ message: 'Copied to clipboard!', type: 'success' })
    setTimeout(() => setCopiedId(null), 2000)
  }

  const handleDelete = async (shortCode) => {
    setDeletingId(shortCode)
    try {
      await deactivateUrl(shortCode)
      removeUrl(shortCode)
      setToast({ message: `/${shortCode} deactivated`, type: 'info' })
    } catch {
      setToast({
        message: 'Failed to deactivate. Please try again.',
        type: 'error',
      })
    } finally {
      setDeletingId(null)
      setConfirmDelete(null)
    }
  }

  return (
    <main
      style={{
        flex: 1,
        width: '100%',
        maxWidth: 900,
        margin: '0 auto',
        padding: '48px 24px',
      }}
    >
      {/* Header */}
      <div
        className="animate-fade-up"
        style={{
          display: 'flex',
          alignItems: 'flex-end',
          justifyContent: 'space-between',
          gap: 16,
          flexWrap: 'wrap',
          marginBottom: 32,
        }}
      >
        <div>
          <h1
            style={{
              fontFamily: 'var(--font-display)',
              fontSize: 32,
              letterSpacing: '-0.02em',
              color: 'var(--ink)',
              marginBottom: 6,
            }}
          >
            My URLs
          </h1>
          <p style={{ fontSize: 14, color: 'var(--stone-400)' }}>
            {urls.length} link{urls.length !== 1 ? 's' : ''} · stored locally in
            your browser
          </p>
        </div>
        <Button onClick={() => navigate('/')} size="sm" style={{ gap: 6 }}>
          <Link2 size={13} strokeWidth={2.5} />
          Shorten new
        </Button>
      </div>

      {/* Search */}
      {urls.length > 0 && (
        <div
          className="animate-fade-up"
          style={{
            position: 'relative',
            marginBottom: 20,
            animationDelay: '60ms',
            animationFillMode: 'both',
          }}
        >
          <Search
            size={14}
            color="var(--stone-400)"
            style={{
              position: 'absolute',
              left: 14,
              top: '50%',
              transform: 'translateY(-50%)',
              pointerEvents: 'none',
            }}
          />
          <input
            placeholder="Search by short code or URL..."
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            style={{
              width: '100%',
              padding: '10px 14px 10px 38px',
              border: '1px solid var(--stone-200)',
              borderRadius: 'var(--radius-md)',
              fontSize: 14,
              fontFamily: 'var(--font-sans)',
              background: '#fff',
              color: 'var(--ink)',
              outline: 'none',
              transition: 'border-color 0.15s ease',
            }}
            onFocus={(e) => (e.target.style.borderColor = 'var(--accent)')}
            onBlur={(e) => (e.target.style.borderColor = 'var(--stone-200)')}
          />
        </div>
      )}

      {/* Empty state — no URLs at all */}
      {urls.length === 0 && (
        <Card
          className="animate-fade-up"
          style={{ animationDelay: '80ms', animationFillMode: 'both' }}
        >
          <Empty
            icon={Link2}
            title="No URLs yet"
            description="Shorten your first link and it will appear here."
            action={
              <Button
                onClick={() => navigate('/')}
                style={{ marginTop: 8, gap: 6 }}
              >
                <Link2 size={13} />
                Shorten a URL
              </Button>
            }
          />
        </Card>
      )}

      {/* Empty state — no search results */}
      {urls.length > 0 && filtered.length === 0 && (
        <Card>
          <Empty
            icon={Search}
            title="No results"
            description={`Nothing matches "${search}"`}
          />
        </Card>
      )}

      {/* URL list */}
      <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
        {filtered.map((url, i) => (
          <Card
            key={url.shortCode}
            className="animate-fade-up"
            style={{
              padding: '20px 24px',
              animationDelay: `${i * 40}ms`,
              animationFillMode: 'both',
              transition: 'box-shadow 0.2s',
            }}
            onMouseEnter={(e) =>
              (e.currentTarget.style.boxShadow = 'var(--shadow-md)')
            }
            onMouseLeave={(e) =>
              (e.currentTarget.style.boxShadow = 'var(--shadow-sm)')
            }
          >
            <div style={{ display: 'flex', alignItems: 'flex-start', gap: 14 }}>
              {/* Icon */}
              <div
                style={{
                  width: 36,
                  height: 36,
                  flexShrink: 0,
                  borderRadius: 'var(--radius-md)',
                  background: 'var(--accent-light)',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  marginTop: 2,
                }}
              >
                <Link2 size={15} color="var(--accent)" strokeWidth={2} />
              </div>

              {/* Content */}
              <div style={{ flex: 1, minWidth: 0 }}>
                {/* Short code */}
                <div
                  style={{
                    display: 'flex',
                    alignItems: 'center',
                    gap: 8,
                    marginBottom: 4,
                  }}
                >
                  <a
                    href={url.shortUrl}
                    target="_blank"
                    rel="noopener noreferrer"
                    style={{
                      fontFamily: 'var(--font-mono)',
                      fontSize: 14,
                      fontWeight: 500,
                      color: 'var(--accent)',
                      textDecoration: 'none',
                    }}
                  >
                    /{url.shortCode}
                  </a>
                  {url.expiresAt && (
                    <span
                      style={{
                        fontSize: 11,
                        fontWeight: 600,
                        color: '#B07A2A',
                        background: '#FBF3E3',
                        padding: '2px 8px',
                        borderRadius: 20,
                        letterSpacing: '0.04em',
                        textTransform: 'uppercase',
                      }}
                    >
                      expires {formatDate(url.expiresAt)}
                    </span>
                  )}
                </div>

                {/* Long URL */}
                <p
                  style={{
                    fontSize: 13,
                    color: 'var(--stone-400)',
                    overflow: 'hidden',
                    textOverflow: 'ellipsis',
                    whiteSpace: 'nowrap',
                    maxWidth: '100%',
                  }}
                >
                  {url.longUrl}
                </p>

                {/* Time */}
                {url.createdAt && (
                  <div
                    style={{
                      display: 'flex',
                      alignItems: 'center',
                      gap: 4,
                      marginTop: 8,
                      fontSize: 12,
                      color: 'var(--stone-400)',
                    }}
                  >
                    <Clock size={11} strokeWidth={2} />
                    {timeAgo(url.createdAt)}
                  </div>
                )}
              </div>

              {/* Actions */}
              <div
                style={{
                  display: 'flex',
                  gap: 6,
                  flexShrink: 0,
                  alignItems: 'center',
                }}
              >
                {/* Copy */}
                <ActionButton
                  title="Copy short URL"
                  onClick={() => copy(url.shortUrl, url.shortCode)}
                >
                  {copiedId === url.shortCode ? (
                    <Check size={13} color="var(--success)" />
                  ) : (
                    <Copy size={13} />
                  )}
                </ActionButton>

                {/* Open */}
                <ActionButton
                  title="Open link"
                  onClick={() => window.open(url.shortUrl, '_blank')}
                >
                  <ExternalLink size={13} />
                </ActionButton>

                {/* Delete */}
                {confirmDelete === url.shortCode ? (
                  <div
                    style={{ display: 'flex', gap: 4, alignItems: 'center' }}
                  >
                    <Button
                      variant="danger"
                      size="sm"
                      loading={deletingId === url.shortCode}
                      onClick={() => handleDelete(url.shortCode)}
                      style={{ fontSize: 12, padding: '4px 10px' }}
                    >
                      Confirm
                    </Button>
                    <Button
                      variant="ghost"
                      size="sm"
                      onClick={() => setConfirmDelete(null)}
                      style={{ fontSize: 12, padding: '4px 10px' }}
                    >
                      Cancel
                    </Button>
                  </div>
                ) : (
                  <ActionButton
                    title="Deactivate"
                    onClick={() => setConfirmDelete(url.shortCode)}
                    danger
                  >
                    <Trash2 size={13} />
                  </ActionButton>
                )}
              </div>
            </div>
          </Card>
        ))}
      </div>

      {toast && (
        <Toast
          message={toast.message}
          type={toast.type}
          onDone={() => setToast(null)}
        />
      )}
    </main>
  )
}

// Small reusable icon button
function ActionButton({ children, onClick, title, danger }) {
  const [hovered, setHovered] = useState(false)

  return (
    <button
      title={title}
      onClick={onClick}
      onMouseEnter={() => setHovered(true)}
      onMouseLeave={() => setHovered(false)}
      style={{
        width: 32,
        height: 32,
        border: `1px solid ${
          hovered
            ? danger
              ? 'var(--error)'
              : 'var(--accent)'
            : 'var(--stone-200)'
        }`,
        borderRadius: 'var(--radius-md)',
        background: '#fff',
        cursor: 'pointer',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        transition: 'var(--transition)',
        color: hovered
          ? danger
            ? 'var(--error)'
            : 'var(--accent)'
          : 'var(--stone-600)',
      }}
    >
      {children}
    </button>
  )
}
