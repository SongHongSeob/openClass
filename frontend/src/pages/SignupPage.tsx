// @MX:NOTE: [AUTO] 회원가입 화면 — REQ-SES-001. 라우터에 의존하지 않는다
// (내비게이션은 `onSignupSuccess` 콜백으로 상위 레이어에 위임 — M2 시점에는
// 라우터가 아직 배선되지 않았다).

import { useState, type FormEvent } from 'react'
import { signup } from '../api/endpoints'
import { ApiError } from '../api/client'

export interface SignupPageProps {
  /** REQ-SES-001 — 성공 시 로그인 화면으로 유도한다. */
  onSignupSuccess: () => void
}

type SubmitState = { status: 'idle' } | { status: 'submitting' } | { status: 'error'; message: string }

export function SignupPage({ onSignupSuccess }: SignupPageProps) {
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [state, setState] = useState<SubmitState>({ status: 'idle' })

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    setState({ status: 'submitting' })
    try {
      await signup({ email, password })
      onSignupSuccess()
    } catch (error) {
      // 오류 문구는 errors.ts 단일 정규화 지점의 결과만 사용한다(REQ-ERR-002) —
      // 여기서 응답 바디를 직접 해석하지 않는다.
      const message = error instanceof ApiError ? error.normalized.message : '회원가입 중 오류가 발생했습니다.'
      setState({ status: 'error', message })
    }
  }

  return (
    <form onSubmit={handleSubmit}>
      <h1>회원가입</h1>
      <label>
        이메일
        <input type="email" required autoComplete="email" value={email} onChange={(event) => setEmail(event.target.value)} />
      </label>
      <label>
        비밀번호
        <input
          type="password"
          required
          minLength={8}
          autoComplete="new-password"
          value={password}
          onChange={(event) => setPassword(event.target.value)}
        />
      </label>
      {state.status === 'error' && <p role="alert">{state.message}</p>}
      <button type="submit" disabled={state.status === 'submitting'}>
        가입하기
      </button>
    </form>
  )
}
