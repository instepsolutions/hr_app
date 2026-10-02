import { useEffect } from 'react'
import { AlertCircle, Check, ChevronDown, ChevronLeft, ChevronRight, LoaderCircle, X } from 'lucide-react'

export function PageLayout({ eyebrow, title, description, action, children, className = '' }) {
  return (
    <main className={`page-content ${className}`}>
      <div className="page-heading">
        <div>
          {eyebrow && <div className="page-eyebrow">{eyebrow}</div>}
          <h1>{title}</h1>
          {description && <p>{description}</p>}
        </div>
        {action && <div className="page-heading-action">{action}</div>}
      </div>
      {children}
    </main>
  )
}

export function StatCard({ label, value, note, icon: Icon, accent = 'blue' }) {
  return (
    <article className={`stat-card stat-${accent}`}>
      <div className="stat-topline"><span>{label}</span><span className="stat-icon"><Icon size={17} /></span></div>
      <div className="stat-value">{value ?? '—'}</div>
      <div className="stat-note">{note}</div>
    </article>
  )
}

export function DataTable({ columns, children, minWidth = '760px', className = '' }) {
  return (
    <div className="table-scroll">
      <table className={className} style={{ minWidth }}>
        <thead><tr>{columns.map((column) => <th className={column.className || ''} key={column.key}>{column.label}</th>)}</tr></thead>
        <tbody>{children}</tbody>
      </table>
    </div>
  )
}

export function LoadingState({ label = 'Loading employee data' }) {
  return <div className="state-panel" role="status"><LoaderCircle className="spinner" size={24} /><span>{label}</span></div>
}

export function ErrorState({ message, onRetry }) {
  return (
    <div className="state-panel error-panel" role="alert">
      <span className="state-icon error-icon"><AlertCircle size={19} /></span>
      <div><strong>We couldn’t load this data</strong><p>{message}</p></div>
      {onRetry && <button className="button button-secondary button-small" onClick={onRetry}>Try again</button>}
    </div>
  )
}

export function EmptyState({ title = 'No employees found', detail = 'Try changing your search or filters.' }) {
  return <div className="empty-state"><span className="empty-mark">—</span><strong>{title}</strong><p>{detail}</p></div>
}

export function SelectFilter({ label, value, onChange, options, placeholder = 'All' }) {
  return (
    <label className="filter-field">
      <span>{label}</span>
      <span className="select-wrap">
        <select value={value} onChange={(event) => onChange(event.target.value)}>
          <option value="">{placeholder}</option>
          {options.map((option) => <option value={option.value} key={option.value}>{option.label}</option>)}
        </select>
        <ChevronDown size={14} />
      </span>
    </label>
  )
}

export function DateFilter({ label, value, onChange }) {
  return <label className="filter-field"><span>{label}</span><input type="date" value={value} onChange={(event) => onChange(event.target.value)} /></label>
}

export function Modal({ title, description, onClose, children, size = 'medium' }) {
  useEffect(() => {
    const onKeyDown = (event) => { if (event.key === 'Escape') onClose() }
    window.addEventListener('keydown', onKeyDown)
    return () => window.removeEventListener('keydown', onKeyDown)
  }, [onClose])

  return (
    <div className="modal-backdrop" role="presentation" onMouseDown={(event) => { if (event.target === event.currentTarget) onClose() }}>
      <section className={`modal modal-${size}`} role="dialog" aria-modal="true" aria-labelledby="modal-title">
        <header className="modal-header"><div><h2 id="modal-title">{title}</h2>{description && <p>{description}</p>}</div><button className="icon-button" onClick={onClose} aria-label="Close dialog"><X size={19} /></button></header>
        {children}
      </section>
    </div>
  )
}

export function Toast({ toast, onClose }) {
  useEffect(() => {
    if (!toast) return undefined
    const timer = window.setTimeout(onClose, 4200)
    return () => window.clearTimeout(timer)
  }, [toast, onClose])
  if (!toast) return null
  return <div className={`toast toast-${toast.type || 'success'}`} role="status"><span>{toast.type === 'error' ? <AlertCircle size={17} /> : <Check size={17} />}</span><p>{toast.message}</p><button onClick={onClose} aria-label="Dismiss notification"><X size={16} /></button></div>
}

export function Pagination({ page, totalPages, totalElements, size, onPageChange, onSizeChange }) {
  const start = totalElements === 0 ? 0 : page * size + 1
  const end = Math.min((page + 1) * size, totalElements)
  return (
    <div className="pagination">
      <div className="pagination-info">Showing <strong>{start}–{end}</strong> of <strong>{totalElements}</strong> employees</div>
      <div className="pagination-controls">
        <label className="page-size-select"><span>Rows</span><select value={size} onChange={(event) => onSizeChange(Number(event.target.value))}><option value={10}>10</option><option value={20}>20</option><option value={50}>50</option></select></label>
        <span className="page-count">Page {totalPages === 0 ? 0 : page + 1} of {totalPages}</span>
        <button className="icon-button page-arrow" onClick={() => onPageChange(page - 1)} disabled={page <= 0} aria-label="Previous page"><ChevronLeft size={17} /></button>
        <button className="icon-button page-arrow" onClick={() => onPageChange(page + 1)} disabled={page + 1 >= totalPages} aria-label="Next page"><ChevronRight size={17} /></button>
      </div>
    </div>
  )
}
