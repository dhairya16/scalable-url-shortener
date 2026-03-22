import React from 'react'
import { NavLink } from 'react-router-dom'
import { Link2, List } from 'lucide-react'

export default function Nav() {
  return (
    <nav
      style={{
        borderBottom: '1px solid var(--stone-100)',
        background: 'rgba(250,250,247,0.92)',
        backdropFilter: 'blur(12px)',
        position: 'sticky',
        top: 0,
        zIndex: 100,
      }}
    >
      <div
        style={{
          maxWidth: 900,
          margin: '0 auto',
          padding: '0 24px',
          height: 58,
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
        }}
      >
        {/* Wordmark */}
        <NavLink
          to="/"
          style={{
            textDecoration: 'none',
            display: 'flex',
            alignItems: 'center',
            gap: 10,
          }}
        >
          <div
            style={{
              width: 28,
              height: 28,
              background: 'var(--accent)',
              borderRadius: 6,
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
            }}
          >
            <Link2 size={14} color="white" strokeWidth={2.5} />
          </div>
          <span
            style={{
              fontFamily: 'var(--font-display)',
              fontSize: 20,
              color: 'var(--ink)',
              letterSpacing: '-0.02em',
            }}
          >
            Shortly
          </span>
        </NavLink>

        {/* Nav links */}
        <div style={{ display: 'flex', gap: 4 }}>
          {[
            { to: '/', label: 'Shorten', icon: Link2 },
            { to: '/urls', label: 'My URLs', icon: List },
          ].map(({ to, label, icon: Icon }) => (
            <NavLink
              key={to}
              to={to}
              end
              style={({ isActive }) => ({
                display: 'flex',
                alignItems: 'center',
                gap: 6,
                padding: '6px 14px',
                borderRadius: 'var(--radius-md)',
                textDecoration: 'none',
                fontSize: 14,
                fontWeight: 500,
                fontFamily: 'var(--font-sans)',
                color: isActive ? 'var(--accent)' : 'var(--stone-600)',
                background: isActive ? 'var(--accent-light)' : 'transparent',
                transition: 'var(--transition)',
              })}
            >
              <Icon size={14} strokeWidth={2} />
              {label}
            </NavLink>
          ))}
        </div>
      </div>
    </nav>
  )
}
