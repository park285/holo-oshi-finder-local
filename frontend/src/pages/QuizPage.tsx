import { useState, useEffect } from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom'
import { Button, Progress, Typography, Card, Checkbox, Space, Row, Col, message } from 'antd'
import { ArrowLeftOutlined, CheckCircleOutlined } from '@ant-design/icons'
import { TWENTY_QUESTIONS } from '../data/questions'
import type { QuizQuestion, QuizAnswer } from '../types'
import API_CONFIG, { apiClient } from '../config/api'

const { Title, Text } = Typography

function QuizPage() {
  const navigate = useNavigate()
  const [searchParams, setSearchParams] = useSearchParams()
  
  // URL에서 현재 질문 인덱스 가져오기
  const qParam = searchParams.get('q')
  const currentQuestionIndex = qParam ? Math.max(0, Math.min(parseInt(qParam) - 1, TWENTY_QUESTIONS.length - 1)) : 0
  
  // 첫 진입 시 URL 파라미터 설정
  useEffect(() => {
    if (!qParam) {
      setSearchParams({ q: '1' }, { replace: true })
    }
  }, [])
  
  // 답변 저장 (인덱스 기반으로 관리)
  const [answers, setAnswers] = useState<QuizAnswer[]>([])
  const [selectedOptions, setSelectedOptions] = useState<string[]>([])
  
  // 질문 변경 시 처리
  useEffect(() => {
    setSelectedOptions([])
    // 뒤로 갔을 때 해당 질문 이후의 답변 제거
    if (answers.length > currentQuestionIndex) {
      setAnswers(prev => prev.slice(0, currentQuestionIndex))
    }
  }, [currentQuestionIndex])

  // 이전 질문으로 돌아가기
  const handleBack = () => {
    if (currentQuestionIndex > 0) {
      setSearchParams({ q: currentQuestionIndex.toString() })
    }
  }
  
  // 다음 질문으로 이동
  const goToNextQuestion = () => {
    setSearchParams({ q: (currentQuestionIndex + 2).toString() })
  }

  const currentQuestion = TWENTY_QUESTIONS[currentQuestionIndex]
  const progress = ((currentQuestionIndex + 1) / TWENTY_QUESTIONS.length) * 100

  const handleSingleAnswer = (optionId: string, optionValue: string) => {
    const newAnswer: QuizAnswer = {
      questionId: currentQuestion.id,
      answer: optionValue
    }
    
    // 현재까지의 답변 + 새 답변
    const updatedAnswers = [...answers.slice(0, currentQuestionIndex), newAnswer]
    setAnswers(updatedAnswers)
    
    console.log('답변 저장:', {
      질문번호: currentQuestionIndex + 1,
      질문ID: currentQuestion.id,
      질문: currentQuestion.question,
      답변: optionValue,
      전체답변수: updatedAnswers.length,
      현재답변목록: updatedAnswers
    })
    
    if (currentQuestionIndex < TWENTY_QUESTIONS.length - 1) {
      goToNextQuestion()
    } else {
      submitAnswers(updatedAnswers)
    }
  }

  const handleMultipleAnswer = () => {
    if (selectedOptions.length === 0) return

    const selectedValues = selectedOptions
      .map(id => currentQuestion.options?.find(opt => opt.id === id)?.value)
      .filter(Boolean) as string[]

    const newAnswer: QuizAnswer = {
      questionId: currentQuestion.id,
      answer: selectedValues
    }

    // 현재까지의 답변 + 새 답변
    const updatedAnswers = [...answers.slice(0, currentQuestionIndex), newAnswer]
    setAnswers(updatedAnswers)
    setSelectedOptions([])
    
    console.log('복수 답변 저장:', {
      질문번호: currentQuestionIndex + 1,
      질문: currentQuestion.question,
      답변: selectedValues,
      전체답변수: updatedAnswers.length
    })

    if (currentQuestionIndex < TWENTY_QUESTIONS.length - 1) {
      goToNextQuestion()
    } else {
      submitAnswers(updatedAnswers)
    }
  }

  const submitAnswers = (finalAnswers: QuizAnswer[]) => {
    console.log('===== 최종 답변 제출 =====')
    console.log('답변 개수:', finalAnswers.length)
    console.log('모든 답변:', finalAnswers)

    // 답변을 임시 저장
    const surveyResponses = finalAnswers.map(answer => {
      const question = TWENTY_QUESTIONS.find(q => q.id === answer.questionId)
      return {
        questionId: answer.questionId.toString(),
        question: question?.question || '',
        answer: Array.isArray(answer.answer) ? answer.answer.join(', ') : answer.answer.toString(),
        importance: question?.weight || 5
      }
    })
    
    // 분석 중 상태 저장
    localStorage.setItem('analysisInProgress', JSON.stringify({
      surveyResponses,
      startTime: new Date().toISOString()
    }))
    
    console.log('백엔드로 전송할 데이터:', {
      surveyResponses,
      analysisDepth: 'detailed'
    })

    // 백엔드 형태로 데이터 변환
    const formattedResponses = surveyResponses.map(response => ({
      questionId: response.questionId.toString(),
      question: TWENTY_QUESTIONS.find(q => q.id === response.questionId)?.question || '',
      answer: Array.isArray(response.answer) 
        ? response.answer.join(', ')  // 배열을 문자열로 변환
        : response.answer.toString(), // 숫자/문자열을 문자열로 변환
      category: TWENTY_QUESTIONS.find(q => q.id === response.questionId)?.category,
      importance: TWENTY_QUESTIONS.find(q => q.id === response.questionId)?.weight
    }))

    // 분석 안내 알림
    message.info('분석이 시작되었습니다. 시간이 1-2분 정도 걸릴 수 있습니다.')
    
    // 즉시 홈으로 이동 (분석은 백그라운드에서 진행)
    navigate('/')
    
    // 백그라운드에서 API 호출 처리
    const apiUrl = `${API_CONFIG.BASE_URL}${API_CONFIG.ENDPOINTS.ANALYZE}`
    console.log('API URL:', apiUrl)
    console.log('Starting background API call...')
    
    fetch(apiUrl, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({
        surveyResponses: formattedResponses,
        analysisDepth: 'detailed'
      })
    })
    .then(response => {
      console.log('Got background response!')
      console.log('Response status:', response.status)
      if (!response.ok) {
        throw new Error(`HTTP ${response.status}: ${response.statusText}`)
      }
      return response.json()
    })
    .then(result => {
      console.log('분석 결과 받음:', result)
      
      // 결과 저장
      localStorage.setItem('lastMatchResult', JSON.stringify({
        ...result,
        timestamp: new Date().toISOString()
      }))
      
      // 분석 중 상태 제거
      localStorage.removeItem('analysisInProgress')
      
      console.log('백그라운드 분석 완료! 결과가 저장되었습니다.')
      
      // 분석 완료 알림
      message.success('분석이 완료되었습니다! 결과를 확인해보세요.')
      
      // 홈페이지 컴포넌트를 리프레시하기 위한 이벤트 발생
      window.dispatchEvent(new Event('analysisComplete'))
    })
    .catch(error => {
      console.error('Background API error:', error)
      console.error('Request data was:', formattedResponses)
      
      localStorage.removeItem('analysisInProgress')
      
      // 에러 알림
      message.error('분석 중 오류가 발생했습니다. 잠시 후 다시 시도해주세요.')
      
      // 에러 상태 저장
      localStorage.setItem('analysisError', JSON.stringify({
        error: error.message,
        timestamp: new Date().toISOString(),
        endpoint: apiUrl,
        requestData: formattedResponses
      }))
    })
  }

  const handleCheckboxChange = (optionId: string, checked: boolean) => {
    if (checked) {
      setSelectedOptions([...selectedOptions, optionId])
    } else {
      setSelectedOptions(selectedOptions.filter(id => id !== optionId))
    }
  }


  return (
    <div className="min-h-screen" style={{ padding: '32px' }}>
      <div style={{ maxWidth: '800px', margin: '0 auto' }}>
        {/* Header */}
        <Space direction="vertical" size="large" style={{ width: '100%', marginBottom: '32px' }}>
          <Space>
            <Button 
              icon={<ArrowLeftOutlined />}
              onClick={() => navigate('/')}
              size="large"
            >
              홈으로
            </Button>
            {currentQuestionIndex > 0 && (
              <Button 
                onClick={handleBack}
                size="large"
              >
                이전 질문
              </Button>
            )}
          </Space>
          
          <div>
            <Title level={2} style={{ marginBottom: '16px' }}>
              나의 오시 찾기
            </Title>
            <Progress 
              percent={progress} 
              showInfo={false}
              strokeColor={{
                '0%': '#27C7FE',
                '100%': '#8B5CF6'
              }}
              style={{ marginBottom: '8px' }}
            />
            <Text type="secondary">
              {currentQuestionIndex + 1} / {TWENTY_QUESTIONS.length}
            </Text>
          </div>
        </Space>

        {/* Question Card */}
        <Card 
          className="glass-effect"
          variant="borderless"
          style={{ padding: '32px' }}
        >
          <Space direction="vertical" size="large" align="center" style={{ width: '100%' }}>
            <Text strong style={{ color: '#27C7FE', fontSize: '18px' }}>
              Q{currentQuestionIndex + 1}
            </Text>
            
            <Title level={3} style={{ textAlign: 'center', marginBottom: '32px' }}>
              {currentQuestion.question}
            </Title>

            {currentQuestion.type === 'single' ? (
              <Space direction="vertical" size="middle" style={{ width: '100%', maxWidth: '500px' }}>
                {currentQuestion.options?.map(option => (
                  <Button
                    key={option.id}
                    size="large"
                    block
                    style={{ 
                      height: 'auto', 
                      padding: '16px 24px',
                      textAlign: 'left',
                      whiteSpace: 'normal'
                    }}
                    onClick={() => handleSingleAnswer(option.id, option.value)}
                  >
                    {option.label}
                  </Button>
                ))}
              </Space>
            ) : (
              <div style={{ width: '100%', maxWidth: '500px' }}>
                <Space direction="vertical" size="middle" style={{ width: '100%' }}>
                  {currentQuestion.options?.map(option => (
                    <Card
                      key={option.id}
                      className={selectedOptions.includes(option.id) ? 'glass-effect' : ''}
                      style={{ 
                        cursor: 'pointer',
                        background: selectedOptions.includes(option.id) 
                          ? 'rgba(39, 199, 254, 0.1)' 
                          : 'rgba(255, 255, 255, 0.05)',
                        borderColor: selectedOptions.includes(option.id) 
                          ? '#27C7FE' 
                          : 'rgba(255, 255, 255, 0.1)'
                      }}
                      onClick={() => handleCheckboxChange(option.id, !selectedOptions.includes(option.id))}
                      variant="outlined"
                    >
                      <Checkbox
                        checked={selectedOptions.includes(option.id)}
                        style={{ width: '100%' }}
                      >
                        <Text style={{ fontSize: '16px', marginLeft: '8px' }}>
                          {option.label}
                        </Text>
                      </Checkbox>
                    </Card>
                  ))}
                </Space>
                
                {selectedOptions.length > 0 && (
                  <Button
                    type="primary"
                    size="large"
                    block
                    style={{ marginTop: '24px' }}
                    icon={<CheckCircleOutlined />}
                    onClick={handleMultipleAnswer}
                  >
                    선택 완료 ({selectedOptions.length}개)
                  </Button>
                )}
              </div>
            )}
          </Space>
        </Card>
      </div>
    </div>
  )
}

export default QuizPage