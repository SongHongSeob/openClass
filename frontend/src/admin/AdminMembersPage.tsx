// @MX:NOTE: [AUTO] 관리자 회원 관리 화면 — `AdminCoursesPage.tsx`와 동일한
// 관례(수동 useState/useEffect/useCallback fetch, LoadState 판별 유니온,
// 행별 busy-id state)를 따른다. ADMIN 자기 자신의 행은 버튼을 disabled로
// 렌더링한다 — 백엔드 409 가드 위에 얹은 UX 장치일 뿐, 실제 강제는 서버가 한다.

import { useCallback, useEffect, useState } from 'react'
import { listMembers, updateMemberRole } from '../api/endpoints'
import { ApiError } from '../api/client'
import type { Member } from '../api/types'
import { Button } from '@/components/ui/button'
import { Card } from '@/components/ui/card'
import { Alert } from '@/components/ui/alert'
import { Badge } from '@/components/ui/badge'

export interface AdminMembersPageProps {
  token: string
  currentUserEmail: string
}

type LoadState =
  | { status: 'loading' }
  | { status: 'error'; message: string }
  | { status: 'loaded'; items: Member[] }

export function AdminMembersPage({ token, currentUserEmail }: AdminMembersPageProps) {
  const [state, setState] = useState<LoadState>({ status: 'loading' })
  const [updatingId, setUpdatingId] = useState<number | null>(null)
  const [actionError, setActionError] = useState<string | null>(null)

  const load = useCallback(() => {
    setState({ status: 'loading' })
    listMembers(token)
      .then((members) => {
        setState({ status: 'loaded', items: members })
      })
      .catch((error: unknown) => {
        const message = error instanceof ApiError ? error.normalized.message : '회원 목록을 불러오지 못했습니다.'
        setState({ status: 'error', message })
      })
  }, [token])

  useEffect(() => {
    load()
  }, [load])

  const handleRoleChange = async (memberId: number, nextRole: 'MEMBER' | 'ADMIN') => {
    setActionError(null)
    setUpdatingId(memberId)
    try {
      const updated = await updateMemberRole(memberId, nextRole, token)
      setState((current) =>
        current.status === 'loaded'
          ? { status: 'loaded', items: current.items.map((item) => (item.id === updated.id ? updated : item)) }
          : current,
      )
    } catch (error) {
      const message = error instanceof ApiError ? error.normalized.message : '역할 변경 중 오류가 발생했습니다.'
      setActionError(message)
    } finally {
      setUpdatingId(null)
    }
  }

  return (
    <section className="flex flex-col gap-4">
      <div className="flex items-center justify-between">
        <h2 className="text-lg font-semibold text-neutral-900 dark:text-neutral-100">관리자 — 회원 관리</h2>
      </div>
      {actionError && <Alert role="alert" tone="error">{actionError}</Alert>}
      {state.status === 'loading' && <p className="text-sm text-neutral-500">불러오는 중…</p>}
      {state.status === 'error' && <Alert role="alert" tone="error">{state.message}</Alert>}
      {state.status === 'loaded' && state.items.length === 0 && (
        <p className="text-sm text-neutral-500">등록된 회원이 없습니다.</p>
      )}
      {state.status === 'loaded' && state.items.length > 0 && (
        <Card className="overflow-x-auto p-0">
          <table className="w-full text-sm">
            <thead>
              <tr className="border-b border-neutral-200 text-left text-neutral-600 dark:border-neutral-800 dark:text-neutral-400">
                <th className="px-4 py-3 font-medium">이메일</th>
                <th className="px-4 py-3 font-medium">이름</th>
                <th className="px-4 py-3 font-medium">역할</th>
                <th className="px-4 py-3 font-medium">가입일</th>
                <th className="px-4 py-3 font-medium">액션</th>
              </tr>
            </thead>
            <tbody>
              {state.items.map((member) => {
                const isSelf = member.email === currentUserEmail
                const nextRole = member.role === 'ADMIN' ? 'MEMBER' : 'ADMIN'
                const actionLabel = member.role === 'ADMIN' ? '일반회원으로 변경' : '관리자로 승격'
                return (
                  <tr key={member.id} className="border-b border-neutral-100 last:border-0 dark:border-neutral-900">
                    <td className="px-4 py-3 text-neutral-700 dark:text-neutral-300">
                      {member.email}
                      {isSelf && <span className="ml-1 text-neutral-500">(나)</span>}
                    </td>
                    <td className="px-4 py-3 text-neutral-700 dark:text-neutral-300">{member.name ?? '-'}</td>
                    <td className="px-4 py-3">
                      <Badge variant={member.role === 'ADMIN' ? 'accent' : 'neutral'}>{member.role}</Badge>
                    </td>
                    <td className="px-4 py-3 text-neutral-700 dark:text-neutral-300">{member.createdAt}</td>
                    <td className="px-4 py-3">
                      <Button
                        type="button"
                        variant="outline"
                        size="sm"
                        className="whitespace-nowrap"
                        disabled={isSelf || updatingId === member.id}
                        onClick={() => handleRoleChange(member.id, nextRole)}
                      >
                        {actionLabel}
                      </Button>
                    </td>
                  </tr>
                )
              })}
            </tbody>
          </table>
        </Card>
      )}
    </section>
  )
}
