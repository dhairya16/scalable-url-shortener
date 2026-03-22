import React from 'react'

// ── Button ────────────────────────────────────────────────────────────
export function Button({
  children,
  variant = 'primary',
  size = 'md',
  loading,
  disabled,
  style,
  onClick,
  type = 'button',
  ...props
}) {
  const base = {
    display: 'inline-flex',
    alignItems: 'center',
    justifyContent: 'center',
    gap: 8,
    fontFamily: 'var(--font-sans)',
    fontWeight: 500,
    cursor: disabled || loading ? 'not-allowed' : 'pointer',
    border: 'none',
    outline: 'none',
    transition: 'var(--transition)',
    borderRadius: 'var(--radius-md)',
    whiteSpace: 'nowrap',
    opacity: disabled ? 0.5 : 1,
  }

  const variants = {
    primary: { background: 'var(--accent)', color: '#fff' },
    secondary: {
      background: 'var(--stone-100)',
      color: 'var(--stone-800)',
      border: '1px solid var(--stone-200)',
    },
    ghost: {
      background: 'transparent',
      color: 'var(--stone-600)',
      border: '1px solid transparent',
    },
    danger: {
      background: 'var(--error-bg)',
      color: 'var(--error)',
      border: '1px solid #F5C4BB',
    },
  }

  const sizes = {
    sm: { padding: '6px 12px', fontSize: 13 },
    md: { padding: '10px 20px', fontSize: 14 },
    lg: { padding: '14px 28px', fontSize: 15 },
  }

  return (
    <button
      type={type}
      onClick={onClick}
      disabled={disabled || loading}
      style={{ ...base, ...variants[variant], ...sizes[size], ...style }}
      {...props}
    >
      {loading && (
        <span
          style={{
            width: 13,
            height: 13,
            border: '2px solid currentColor',
            borderTopColor: 'transparent',
            borderRadius: '50%',
            display: 'inline-block',
            animation: 'spin 0.8s linear infinite',
          }}
        />
      )}
      {children}
    </button>
  )
}

// ── Input ─────────────────────────────────────────────────────────────
export function Input({ label, error, hint, style, containerStyle, ...props }) {
  const [focused, setFocused] = React.useState(false)

  return (
    <div
      style={{
        display: 'flex',
        flexDirection: 'column',
        gap: 6,
        ...containerStyle,
      }}
    >
      {label && (
        <label
          style={{
            fontSize: 13,
            fontWeight: 500,
            color: 'var(--stone-600)',
            letterSpacing: '0.01em',
          }}
        >
          {label}
        </label>
      )}
      <input
        onFocus={() => setFocused(true)}
        onBlur={() => setFocused(false)}
        style={{
          fontFamily: 'var(--font-sans)',
          fontSize: 14,
          padding: '10px 14px',
          border: `1px solid ${
            error
              ? 'var(--error)'
              : focused
                ? 'var(--accent)'
                : 'var(--stone-200)'
          }`,
          borderRadius: 'var(--radius-md)',
          background: '#fff',
          color: 'var(--ink)',
          outline: 'none',
          transition: 'border-color 0.15s ease',
          width: '100%',
          ...style,
        }}
        {...props}
      />
      {error && (
        <span style={{ fontSize: 12, color: 'var(--error)' }}>{error}</span>
      )}
      {hint && !error && (
        <span style={{ fontSize: 12, color: 'var(--stone-400)' }}>{hint}</span>
      )}
    </div>
  )
}

// ── Card ──────────────────────────────────────────────────────────────
export function Card({ children, style, ...props }) {
  return (
    <div
      style={{
        background: '#fff',
        border: '1px solid var(--stone-100)',
        borderRadius: 'var(--radius-lg)',
        boxShadow: 'var(--shadow-sm)',
        ...style,
      }}
      {...props}
    >
      {children}
    </div>
  )
}

// ── Badge ─────────────────────────────────────────────────────────────
export function Badge({ children, variant = 'default' }) {
  const variants = {
    default: { bg: 'var(--stone-100)', color: 'var(--stone-600)' },
    success: { bg: 'var(--success-bg)', color: 'var(--success)' },
    error: { bg: 'var(--error-bg)', color: 'var(--error)' },
    accent: { bg: 'var(--accent-light)', color: 'var(--accent-dark)' },
  }
  const v = variants[variant] || variants.default

  return (
    <span
      style={{
        display: 'inline-flex',
        alignItems: 'center',
        padding: '2px 8px',
        borderRadius: 20,
        fontSize: 11,
        fontWeight: 600,
        letterSpacing: '0.04em',
        textTransform: 'uppercase',
        background: v.bg,
        color: v.color,
      }}
    >
      {children}
    </span>
  )
}

// ── Toast ─────────────────────────────────────────────────────────────
export function Toast({ message, type = 'success', onDone }) {
  React.useEffect(() => {
    const t = setTimeout(onDone, 3000)
    return () => clearTimeout(t)
  }, [onDone])

  const types = {
    success: {
      bg: 'var(--success-bg)',
      color: 'var(--success)',
      border: '#BFE0CE',
    },
    error: { bg: 'var(--error-bg)', color: 'var(--error)', border: '#F5C4BB' },
    info: {
      bg: 'var(--accent-light)',
      color: 'var(--accent-dark)',
      border: '#E0C8C0',
    },
  }
  const t = types[type] || types.info

  return (
    <div
      style={{
        position: 'fixed',
        bottom: 24,
        right: 24,
        zIndex: 1000,
        padding: '12px 20px',
        background: t.bg,
        color: t.color,
        border: `1px solid ${t.border}`,
        borderRadius: 'var(--radius-md)',
        fontSize: 14,
        fontWeight: 500,
        boxShadow: 'var(--shadow-md)',
        animation: 'fadeUp 0.3s ease',
        maxWidth: 360,
      }}
    >
      {message}
    </div>
  )
}

// ── Empty state ───────────────────────────────────────────────────────
export function Empty({ icon: Icon, title, description, action }) {
  return (
    <div
      style={{
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        justifyContent: 'center',
        padding: '64px 24px',
        gap: 12,
        textAlign: 'center',
      }}
    >
      {Icon && (
        <div
          style={{
            width: 48,
            height: 48,
            borderRadius: '50%',
            background: 'var(--stone-100)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            marginBottom: 4,
          }}
        >
          <Icon size={20} color="var(--stone-400)" strokeWidth={1.5} />
        </div>
      )}
      <p style={{ fontWeight: 500, color: 'var(--stone-800)', fontSize: 15 }}>
        {title}
      </p>
      {description && (
        <p style={{ fontSize: 13, color: 'var(--stone-400)', maxWidth: 280 }}>
          {description}
        </p>
      )}
      {action}
    </div>
  )
}

// ── Spinner ───────────────────────────────────────────────────────────
export function Spinner({ size = 20 }) {
  return (
    <div
      style={{
        width: size,
        height: size,
        border: '2px solid var(--stone-200)',
        borderTopColor: 'var(--accent)',
        borderRadius: '50%',
        animation: 'spin 0.8s linear infinite',
      }}
    />
  )
}
