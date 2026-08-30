// @MX:NOTE: [AUTO] 로그인 화면 — REQ-SES-002. 성공 시 `useSession().
// establishSession`으로 세션을 수립한다(sessionStorage 기록 + 페이로드
// 디코드는 SessionContext가 담당 — 이 화면은 토큰 문자열만 넘긴다).

import { useState, type FormEvent } from 'react'
import { login } from '../api/endpoints'
import { ApiError } from '../api/client'
import { useSession } from '../session/useSession'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Alert } from '@/components/ui/alert'
import { Card } from '@/components/ui/card'

export interface LoginPageProps {
  /** 로그인 성공 후 다음 화면으로 이동시킨다. 내비게이션 수단은 상위 레이어가
   * 결정한다 — 이 컴포넌트는 라우터에 의존하지 않는다. */
  onLoginSuccess: () => void
}

type SubmitState = { status: 'idle' } | { status: 'submitting' } | { status: 'error'; message: string }

export function LoginPage({ onLoginSuccess }: LoginPageProps) {
  const { establishSession } = useSession()
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [state, setState] = useState<SubmitState>({ status: 'idle' })

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    setState({ status: 'submitting' })
    try {
      const result = await login({ email, password })
      establishSession(result.accessToken)
      onLoginSuccess()
    } catch (error) {
      const message = error instanceof ApiError ? error.normalized.message : '로그인 중 오류가 발생했습니다.'
      setState({ status: 'error', message })
    }
  }

  return (
    <Card className="max-w-md">
      <form onSubmit={handleSubmit} className="flex flex-col gap-4">
        <h1 className="text-xl font-semibold text-neutral-900 dark:text-neutral-100">로그인</h1>
        <Label>
          이메일
          <Input
            type="email"
            required
            autoComplete="email"
            value={email}
            onChange={(event) => setEmail(event.target.value)}
          />
        </Label>
        <Label>
          비밀번호
          <Input
            type="password"
            required
            autoComplete="current-password"
            value={password}
            onChange={(event) => setPassword(event.target.value)}
          />
        </Label>
        {state.status === 'error' && <Alert role="alert" tone="error">{state.message}</Alert>}
        <Button type="submit" disabled={state.status === 'submitting'}>
          로그인
        </Button>
      </form>
    </Card>
  )
}
