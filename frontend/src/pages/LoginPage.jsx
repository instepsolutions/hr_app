import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { ArrowRight, Building2, LoaderCircle, LockKeyhole, UserRound } from 'lucide-react'
import { getApiErrorMessage } from '../api/client'

export default function LoginPage({ onLogin }) {
  const [error, setError] = useState('')
  const { register, handleSubmit, formState: { isSubmitting } } = useForm({ defaultValues: { username: '', password: '' } })

  async function submit(credentials) {
    setError('')
    try {
      await onLogin(credentials)
    } catch (requestError) {
      setError(getApiErrorMessage(requestError, 'Sign-in failed. Check your credentials.'))
    }
  }

  return (
    <main className="login-screen">
      <section className="login-panel">
        <div className="login-brand"><span className="brand-mark"><Building2 size={20} /></span><span>northstar<small>PEOPLE OPERATIONS</small></span></div>
        <div className="login-content">
          <span className="login-kicker">EMPLOYEE MANAGEMENT</span>
          <h1>Good work<br />starts with people.</h1>
          <p className="login-copy">Sign in to continue to your people workspace.</p>
          <form className="login-form" onSubmit={handleSubmit(submit)}>
            <label className="form-field"><span>Username</span><span className="input-icon"><UserRound size={16} /><input autoComplete="username" placeholder="Your username" {...register('username', { required: true })} /></span></label>
            <label className="form-field"><span>Password</span><span className="input-icon"><LockKeyhole size={16} /><input type="password" autoComplete="current-password" placeholder="Your password" {...register('password', { required: true })} /></span></label>
            {error && <div className="inline-error" role="alert">{error}</div>}
            <button className="button button-primary login-submit" disabled={isSubmitting}>{isSubmitting ? <LoaderCircle size={17} className="spinner" /> : <>Sign in <ArrowRight size={17} /></>}</button>
          </form>
        </div>
        <div className="login-footer"><span>Northstar HRMS</span><span>Secure employee workspace</span></div>
      </section>
      <aside className="login-art" aria-hidden="true">
        <div className="art-caption"><span className="art-caption-line" /><span>PEOPLE FIRST. ALWAYS.</span></div>
        <div className="art-orbit orbit-one" /><div className="art-orbit orbit-two" />
        <div className="art-quote"><div className="quote-mark">“</div><p>Better work<br />begins with<br /><em>belonging.</em></p><div className="quote-rule" /></div>
        <div className="art-footer"><span>01</span><span>EMPLOYEE EXPERIENCE</span></div>
      </aside>
    </main>
  )
}
