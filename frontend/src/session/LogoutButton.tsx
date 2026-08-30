// @MX:NOTE: [AUTO] 로그아웃 UI — REQ-SES-005(폐기)·REQ-SES-006(문구 제약)의
// 유일한 구현 지점. 문구는 서버 측 무효화를 암시하지 않는다.

import { useSession } from './useSession'
import { Button } from '@/components/ui/button'

// REQ-SES-006 / INV-FE-004 — "로그아웃되었습니다(서버 무효화)" 같은 표현을
// 사용하지 않는다. 백엔드에 denylist가 없으므로(README.md), 로그아웃은
// 클라이언트가 보유 토큰을 버리는 것으로만 이루어지며 토큰은 만료 시각까지
// 서버에서 유효하게 남는다 — 이 사실을 사용자가 확인할 수 있어야 한다.
const LOGOUT_NOTICE =
  '로그아웃되었습니다. 이 브라우저 탭에 저장된 로그인 정보가 제거됩니다. 서버에 남은 토큰 자체는 만료 시각까지 유효합니다(서버 측 강제 무효화 아님).'

export function LogoutButton() {
  const { discardSession, session } = useSession()

  if (session.status !== 'authenticated') {
    return null
  }

  return (
    <div className="flex flex-col gap-2">
      <Button type="button" variant="outline" size="sm" className="w-fit" onClick={discardSession}>
        로그아웃
      </Button>
      <p className="text-xs text-neutral-500 dark:text-neutral-400">{LOGOUT_NOTICE}</p>
    </div>
  )
}
