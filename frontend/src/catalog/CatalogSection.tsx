// @MX:NOTE: [AUTO] 목록·상세 전환을 담당하는 컨테이너(REQ-CAT-004). 과업
// 지시 B1에 따라 라우터를 도입하지 않고 App.tsx의 기존 콜백 기반 화면 전환
// 패턴을 그대로 확장한다 — 지역 상태(useState)로 목록/상세를 전환한다.

import { useState } from 'react'
import { CourseListPage } from './CourseListPage'
import { CourseDetailPage } from './CourseDetailPage'

type CatalogView = { screen: 'list' } | { screen: 'detail'; courseId: number }

export function CatalogSection() {
  const [view, setView] = useState<CatalogView>({ screen: 'list' })

  if (view.screen === 'detail') {
    return <CourseDetailPage courseId={view.courseId} onBack={() => setView({ screen: 'list' })} />
  }

  return <CourseListPage onSelectCourse={(courseId) => setView({ screen: 'detail', courseId })} />
}
