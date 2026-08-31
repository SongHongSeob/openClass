// @MX:NOTE: [AUTO] 내 수강신청 목록·상세 전환을 담당하는 컨테이너. `catalog/CatalogSection.tsx`의
// 목록/상세 지역 상태(useState) 전환 패턴을 그대로 적용한다 — 라우터를
// 도입하지 않고 콜백 기반으로 화면을 전환한다.

import { useState } from 'react'
import { MyEnrollmentsPage } from './MyEnrollmentsPage'
import { CourseDetailPage } from '../catalog/CourseDetailPage'

export interface MyEnrollmentsSectionProps {
  token: string
}

type MyEnrollmentsView = { screen: 'list' } | { screen: 'detail'; courseId: number }

export function MyEnrollmentsSection({ token }: MyEnrollmentsSectionProps) {
  const [view, setView] = useState<MyEnrollmentsView>({ screen: 'list' })

  if (view.screen === 'detail') {
    return <CourseDetailPage courseId={view.courseId} onBack={() => setView({ screen: 'list' })} />
  }

  return (
    <MyEnrollmentsPage
      token={token}
      onSelectCourse={(courseId) => setView({ screen: 'detail', courseId })}
    />
  )
}
