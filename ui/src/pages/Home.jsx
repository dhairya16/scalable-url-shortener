import React, { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import {
  Link2,
  ArrowRight,
  Copy,
  Check,
  Zap,
  Shield,
  BarChart2,
} from 'lucide-react'
import { shortenUrl } from '../lib/api'
import { useUrlHistory } from '../hooks/useUrlHistory'
import { Button, Input, Card, Toast } from '../components/ui'

export default function Home() {
  const navigate = useNavigate()
  const { addUrl } = useUrlHistory()

  const [longUrl, setLongUrl] = useState('')
  const [customCode, setCustomCode] = useState('')
  const [ttlDays, setTtlDays] = useState('')
  const [result, setResult] = useState(null)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const [copied, setCopied] = useState(false)
  const [toast, setToast] = useState(null)
  const [showAdvanced, setShowAdvanced] = useState(false)

  const handleSubmit = async (e) => {
    e.preventDefault()
    if (!longUrl.trim()) {
      setError('Please enter a URL')
      return
    }

    setError('')
    setLoading(true)
    setResult(null)

    try {
      const data = await shortenUrl({
        longUrl: longUrl.trim(),
        customCode: customCode.trim() || undefined,
        ttlDays: ttlDays ? parseInt(ttlDays) : undefined,
      })
      setResult(data)
      addUrl({ ...data, createdAt: new Date().toISOString() })
      setLongUrl('')
      setCustomCode('')
      setTtlDays('')
    } catch (err) {
      const msg =
        err.response?.data?.message || 'Something went wrong. Please try again.'
      setError(msg)
    } finally {
      setLoading(false)
    }
  }

  const copy = async (text) => {
    await navigator.clipboard.writeText(text)
    setCopied(true)
    setToast({ message: 'Copied to clipboard!', type: 'success' })
    setTimeout(() => setCopied(false), 2000)
  }

  return (
    <main style={{ flex: 1 }}>
      {/* Hero */}
      <section
        style={{
          maxWidth: 900,
          margin: '0 auto',
          padding: '72px 24px 40px',
        }}
      >
        <div className="animate-fade-up">
          <div
            style={{
              display: 'inline-flex',
              alignItems: 'center',
              gap: 6,
              padding: '4px 12px',
              background: 'var(--accent-light)',
              borderRadius: 20,
              marginBottom: 20,
              fontSize: 11,
              fontWeight: 600,
              color: 'var(--accent-dark)',
              letterSpacing: '0.06em',
              textTransform: 'uppercase',
            }}
          >
            <Zap size={11} strokeWidth={2.5} />
            Fast · Reliable · Self-hosted
          </div>

          <h1
            style={{
              fontFamily: 'var(--font-display)',
              fontSize: 'clamp(32px, 5vw, 50px)',
              lineHeight: 1.1,
              letterSpacing: '-0.02em',
              color: 'var(--ink)',
              marginBottom: 16,
            }}
          >
            Shorten any link,
            <br />
            <em style={{ color: 'var(--accent)', fontStyle: 'italic' }}>
              track every click.
            </em>
          </h1>

          <p
            style={{
              fontSize: 16,
              color: 'var(--stone-600)',
              lineHeight: 1.7,
              fontWeight: 300,
              maxWidth: 400,
            }}
          >
            A fast, self-hosted URL shortener with real-time analytics. No
            accounts, no limits.
          </p>
        </div>
      </section>

      {/* Shortener card */}
      <section
        style={{
          maxWidth: 900,
          margin: '0 auto',
          padding: '0 24px 80px',
        }}
      >
        <Card
          className="animate-fade-up"
          style={{
            padding: 28,
            animationDelay: '80ms',
            animationFillMode: 'both',
          }}
        >
          <form onSubmit={handleSubmit}>
            {/* Main input row */}
            <div style={{ display: 'flex', gap: 10, alignItems: 'flex-end' }}>
              <Input
                label="Paste your long URL"
                type="url"
                placeholder="https://example.com/very/long/path"
                value={longUrl}
                onChange={(e) => {
                  setLongUrl(e.target.value)
                  setError('')
                }}
                error={error}
                containerStyle={{ flex: 1 }}
              />
              <Button
                type="submit"
                loading={loading}
                style={{ height: 42, flexShrink: 0, gap: 6 }}
              >
                Shorten
                <ArrowRight size={14} strokeWidth={2.5} />
              </Button>
            </div>

            {/* Advanced toggle */}
            <button
              type="button"
              onClick={() => setShowAdvanced((v) => !v)}
              style={{
                marginTop: 10,
                background: 'none',
                border: 'none',
                fontSize: 12,
                color: 'var(--stone-400)',
                cursor: 'pointer',
                padding: 0,
                fontFamily: 'var(--font-sans)',
                transition: 'var(--transition)',
              }}
              onMouseEnter={(e) =>
                (e.currentTarget.style.color = 'var(--stone-600)')
              }
              onMouseLeave={(e) =>
                (e.currentTarget.style.color = 'var(--stone-400)')
              }
            >
              {showAdvanced ? '− Hide' : '+ Show'} advanced options
            </button>

            {/* Advanced fields */}
            {showAdvanced && (
              <div
                className="animate-fade-up"
                style={{
                  display: 'grid',
                  gridTemplateColumns: '1fr 1fr',
                  gap: 16,
                  marginTop: 16,
                }}
              >
                <Input
                  label="Custom alias (optional)"
                  placeholder="my-brand"
                  value={customCode}
                  onChange={(e) => setCustomCode(e.target.value)}
                  hint="3–20 alphanumeric characters or hyphens"
                />
                <Input
                  label="Expiry in days (optional)"
                  type="number"
                  placeholder="365"
                  value={ttlDays}
                  onChange={(e) => setTtlDays(e.target.value)}
                  hint="Defaults to 365 days"
                />
              </div>
            )}
          </form>

          {/* Result */}
          {result && (
            <div
              className="animate-fade-up"
              style={{
                marginTop: 20,
                padding: '16px 20px',
                background: 'var(--success-bg)',
                border: '1px solid #BFE0CE',
                borderRadius: 'var(--radius-md)',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'space-between',
                gap: 16,
                flexWrap: 'wrap',
              }}
            >
              <div>
                <p
                  style={{
                    fontSize: 11,
                    fontWeight: 600,
                    color: 'var(--success)',
                    letterSpacing: '0.06em',
                    textTransform: 'uppercase',
                    marginBottom: 4,
                  }}
                >
                  ✓ Link shortened
                </p>
                <a
                  href={result.shortUrl}
                  target="_blank"
                  rel="noopener noreferrer"
                  style={{
                    fontFamily: 'var(--font-mono)',
                    fontSize: 15,
                    fontWeight: 500,
                    color: 'var(--stone-800)',
                    textDecoration: 'none',
                  }}
                >
                  {result.shortUrl}
                </a>
              </div>

              <div style={{ display: 'flex', gap: 8 }}>
                <Button
                  variant="secondary"
                  size="sm"
                  onClick={() => copy(result.shortUrl)}
                  style={{ gap: 6 }}
                >
                  {copied ? (
                    <Check size={13} color="var(--success)" />
                  ) : (
                    <Copy size={13} />
                  )}
                  {copied ? 'Copied' : 'Copy'}
                </Button>
                <Button
                  variant="ghost"
                  size="sm"
                  onClick={() => navigate('/urls')}
                >
                  View all
                </Button>
              </div>
            </div>
          )}
        </Card>

        {/* Feature pills */}
        <div
          className="animate-fade-up"
          style={{
            display: 'flex',
            gap: 10,
            marginTop: 20,
            flexWrap: 'wrap',
            animationDelay: '160ms',
            animationFillMode: 'both',
          }}
        >
          {[
            { icon: Zap, label: 'Sub-millisecond redirects via Redis' },
            { icon: BarChart2, label: 'Real-time analytics in Grafana' },
            { icon: Shield, label: 'Rate limited & self-hosted' },
          ].map(({ icon: Icon, label }) => (
            <div
              key={label}
              style={{
                display: 'flex',
                alignItems: 'center',
                gap: 7,
                padding: '7px 14px',
                background: '#fff',
                border: '1px solid var(--stone-100)',
                borderRadius: 20,
                fontSize: 12.5,
                color: 'var(--stone-600)',
                boxShadow: 'var(--shadow-sm)',
              }}
            >
              <Icon size={12} color="var(--accent)" strokeWidth={2} />
              {label}
            </div>
          ))}
        </div>
      </section>

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
