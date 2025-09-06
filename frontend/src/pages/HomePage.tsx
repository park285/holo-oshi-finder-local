import { useState, useEffect } from 'react'
import { Button, Typography, Space, Switch, Card, Row, Col, Avatar, Progress, Tag, Divider, Spin } from 'antd'
import { PlayCircleOutlined, BulbOutlined, BarChartOutlined, TrophyOutlined, ReloadOutlined, HeartOutlined, LoadingOutlined } from '@ant-design/icons'
import { useNavigate } from 'react-router-dom'
import type { AnalysisResponse } from '../types'

const { Title, Paragraph, Text } = Typography

interface HomePageProps {
  onThemeToggle: () => void
  isDarkMode: boolean
}

function HomePage({ onThemeToggle, isDarkMode }: HomePageProps) {
  const navigate = useNavigate()
  const [lastResult, setLastResult] = useState<AnalysisResponse | null>(null)
  const [isAnalyzing, setIsAnalyzing] = useState(false)
  const [isServiceReady, setIsServiceReady] = useState(false)
  const [apiTestResult, setApiTestResult] = useState<string>('')
  const [isTestingApi, setIsTestingApi] = useState(false)
  
  // 색상 테마
  const colors = {
    primary: {
      gradient: 'linear-gradient(90deg, #FFB7C5, #FF85C0)',
      shadow: 'rgba(255, 183, 197, 0.3)'
    },
    disabled: {
      background: '#d9d9d9',
      text: '#999'
    }
  }

  // localStorage에서 마지막 결과 불러오기 & 분석 중 상태 확인
  useEffect(() => {
    // 분석 중인지 확인
    const analysisInProgress = localStorage.getItem('analysisInProgress')
    if (analysisInProgress) {
      setIsAnalyzing(true)
    }
    
    // 저장된 결과 불러오기
    const savedResult = localStorage.getItem('lastMatchResult')
    if (savedResult) {
      try {
        setLastResult(JSON.parse(savedResult))
      } catch (e) {
        console.error('Failed to parse saved result:', e)
      }
    }
    
    // 분석 완료 이벤트 리스너
    const handleAnalysisComplete = () => {
      setIsAnalyzing(false)
      const newResult = localStorage.getItem('lastMatchResult')
      if (newResult) {
        try {
          setLastResult(JSON.parse(newResult))
        } catch (e) {
          console.error('Failed to parse new result:', e)
        }
      }
    }
    
    window.addEventListener('analysisComplete', handleAnalysisComplete)
    
    return () => {
      window.removeEventListener('analysisComplete', handleAnalysisComplete)
    }
  }, [])

  return (
    <div className="min-h-screen flex flex-col">
      {/* Responsive Header */}
      <header style={{ 
        display: 'flex', 
        justifyContent: 'center', 
        alignItems: 'center', 
        padding: '20px 24px',
        position: 'sticky',
        top: 0,
        zIndex: 10,
        background: isDarkMode ? 'rgba(26, 26, 46, 0.95)' : 'rgba(255, 255, 255, 0.95)',
        backdropFilter: 'blur(10px)',
        borderBottom: '1px solid rgba(255, 255, 255, 0.1)'
      }}>
        <div style={{
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          width: '100%',
          maxWidth: '1200px'
        }}>
          <div style={{ width: '40px' }} />
          <Title level={2} style={{ margin: 0, textAlign: 'center' }} className="gradient-text">
            Holo-Oshi Finder
          </Title>
          <Switch 
            checkedChildren="🌙" 
            unCheckedChildren="☀️" 
            checked={isDarkMode}
            onChange={onThemeToggle}
          />
        </div>
      </header>

      {/* Responsive Main Content */}
      <main style={{ 
        flex: 1, 
        display: 'flex', 
        flexDirection: 'column', 
        alignItems: 'center', 
        justifyContent: 'center', 
        padding: '60px 20px'
      }}>
        <div style={{ 
          textAlign: 'center', 
          maxWidth: '1200px', 
          width: '100%' 
        }}>
          {/* Hero Section */}
          <Title level={1} style={{ 
            marginBottom: '16px',
            fontSize: 'clamp(36px, 6vw, 56px)',
            fontWeight: 700
          }}>
            <span style={{ color: '#FFB7C5' }}>Holo-Oshi</span>
            <span style={{ color: isDarkMode ? '#ffffff' : '#1a1a2e' }}> Finder</span>
          </Title>
          <Paragraph style={{ 
            fontSize: 'clamp(16px, 2vw, 18px)', 
            marginBottom: '48px', 
            opacity: 0.7,
            maxWidth: '500px',
            margin: '0 auto 48px'
          }}>
            AI가 분석하는 당신만의 홀로라이브 최애 멤버 찾기
          </Paragraph>

          {/* Main Feature Card + Side Cards Layout */}
          <Row gutter={32} justify="center" style={{ maxWidth: '900px', margin: '0 auto' }}>
            {/* Left Main Card */}
            <Col xs={24} md={12}>
              <Card 
                className="glass-effect"
                variant="borderless"
                style={{ 
                  height: '380px',
                  background: isDarkMode 
                    ? 'linear-gradient(135deg, rgba(255,255,255,0.05), rgba(255,255,255,0.02))' 
                    : 'linear-gradient(135deg, rgba(255,255,255,0.95), rgba(255,255,255,0.9))',
                  backdropFilter: 'blur(10px)',
                  borderRadius: '20px',
                  boxShadow: '0 10px 40px rgba(0, 0, 0, 0.1)'
                }}
                styles={{ 
                  body: {
                    padding: '48px 40px',
                    height: '100%',
                    display: 'flex',
                    flexDirection: 'column',
                    justifyContent: 'space-between'
                  }
                }}
              >
                <div>
                  <div style={{ 
                    width: '90px', 
                    height: '90px', 
                    margin: '0 auto 28px',
                    background: 'linear-gradient(135deg, rgba(255,183,197,0.15), rgba(255,214,102,0.1))',
                    borderRadius: '20px',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center'
                  }}>
                    <PlayCircleOutlined style={{ 
                      fontSize: '46px', 
                      background: 'linear-gradient(135deg, #FFB7C5, #FFD666)',
                      WebkitBackgroundClip: 'text',
                      WebkitTextFillColor: 'transparent'
                    }} />
                  </div>
                  
                  <Title level={3} style={{ 
                    fontSize: '26px', 
                    marginBottom: '16px',
                    fontWeight: 700,
                    color: isDarkMode ? '#ffffff' : '#1a1a2e'
                  }}>
                    나의 오시 찾기
                  </Title>
                  <Paragraph style={{ 
                    fontSize: '15px', 
                    lineHeight: '1.6',
                    opacity: 0.7,
                    marginBottom: 0
                  }}>
                    20개의 질문으로 당신과 가장 잘 맞는<br/>
                    홀로라이브 멤버를 AI가 찾아드립니다
                  </Paragraph>
                </div>
                
                <Button 
                  size="large" 
                  block
                  disabled={!isServiceReady}
                  style={{ 
                    height: '52px',
                    background: isServiceReady 
                      ? 'linear-gradient(90deg, #FFB7C5, #FF85C0)'
                      : '#d9d9d9',
                    border: 'none',
                    color: isServiceReady ? 'white' : '#999',
                    fontSize: '16px',
                    fontWeight: 600,
                    borderRadius: '12px',
                    boxShadow: isServiceReady 
                      ? '0 4px 16px rgba(255, 183, 197, 0.3)'
                      : 'none',
                    cursor: isServiceReady ? 'pointer' : 'not-allowed'
                  }}
                  onClick={() => navigate('/quiz')}
                >
                  {isServiceReady ? '시작하기 →' : '시작하기 → (준비 중)'}
                </Button>
              </Card>
            </Col>
            
            {/* Right Side Cards */}
            <Col xs={24} md={12}>
              <Space direction="vertical" size={20} style={{ width: '100%' }}>
                {/* 새로운 기능 Card */}
                <Card 
                  className="glass-effect"
                  variant="borderless"
                  style={{ 
                    height: '175px',
                    background: isDarkMode 
                      ? 'rgba(255,255,255,0.03)' 
                      : 'rgba(255,255,255,0.9)',
                    borderRadius: '16px',
                    boxShadow: '0 4px 20px rgba(0, 0, 0, 0.08)'
                  }}
                  styles={{ 
                    body: { 
                      padding: '28px',
                      height: '100%',
                      display: 'flex',
                      alignItems: 'center'
                    }
                  }}
                >
                  <Space align="start" size={20} style={{ width: '100%' }}>
                    <div style={{
                      minWidth: '48px',
                      height: '48px',
                      background: 'linear-gradient(135deg, rgba(255,183,197,0.1), rgba(133,165,255,0.1))',
                      borderRadius: '12px',
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'center'
                    }}>
                      <span style={{ fontSize: '28px' }}>🆕</span>
                    </div>
                    <div style={{ flex: 1 }}>
                      <Title level={4} style={{ marginBottom: '8px', fontSize: '18px', fontWeight: 600 }}>
                        새로운 기능
                      </Title>
                      <Paragraph style={{ 
                        margin: '0 0 8px 0', 
                        fontSize: '14px',
                        opacity: 0.7,
                        lineHeight: '1.5'
                      }}>
                        새로운 기능을 주기적으로<br/>
                        업데이트 예정입니다
                      </Paragraph>
                      <Text style={{ fontSize: '13px', opacity: 0.5 }}>
                        기획 중 0%
                      </Text>
                    </div>
                  </Space>
                </Card>

                {/* 오시 분석 리포트 Card */}
                <Card 
                  className="glass-effect"
                  variant="borderless"
                  style={{ 
                    height: '175px',
                    background: isDarkMode 
                      ? 'rgba(255,255,255,0.03)' 
                      : 'rgba(255,255,255,0.9)',
                    borderRadius: '16px',
                    boxShadow: '0 4px 20px rgba(0, 0, 0, 0.08)'
                  }}
                  styles={{ 
                    body: { 
                      padding: '28px',
                      height: '100%',
                      display: 'flex',
                      alignItems: 'center'
                    }
                  }}
                >
                  <Space align="start" size={20} style={{ width: '100%' }}>
                    <div style={{
                      minWidth: '48px',
                      height: '48px',
                      background: 'linear-gradient(135deg, rgba(149,222,100,0.1), rgba(255,183,197,0.1))',
                      borderRadius: '12px',
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'center'
                    }}>
                      <BarChartOutlined style={{ 
                        fontSize: '28px', 
                        color: '#52C41A'
                      }} />
                    </div>
                    <div style={{ flex: 1 }}>
                      <Title level={4} style={{ marginBottom: '8px', fontSize: '18px', fontWeight: 600 }}>
                        오시 분석 리포트
                      </Title>
                      <Paragraph style={{ 
                        margin: '0 0 12px 0', 
                        fontSize: '14px',
                        opacity: 0.7,
                        lineHeight: '1.5'
                      }}>
                        당신의 취향을 상세하게<br/>
                        분석한 리포트 제공
                      </Paragraph>
                      <Progress 
                        percent={20}
                        size="small"
                        strokeColor={{
                          '0%': '#95DE64',
                          '100%': '#FFB7C5'
                        }}
                        style={{ marginBottom: '4px' }}
                      />
                    </div>
                  </Space>
                </Card>
              </Space>
            </Col>
          </Row>

          {/* 나의 오시 매칭 결과 - Vue 디자인처럼 작은 카드로 */}
          <div style={{ marginTop: '48px', width: '100%', maxWidth: '900px', margin: '48px auto 0' }}>
            {isAnalyzing ? (
              // 분석 중 상태 표시
              <Card 
                className="glass-effect"
                variant="borderless"
                style={{ 
                  background: isDarkMode 
                    ? 'linear-gradient(135deg, rgba(255,183,197,0.08), rgba(255,133,192,0.05))' 
                    : 'linear-gradient(135deg, rgba(255,245,247,0.95), rgba(255,255,255,0.9))',
                  borderRadius: '16px',
                  border: '1px solid rgba(255,183,197,0.2)'
                }}
                styles={{ 
                  body: { padding: '32px', textAlign: 'center' }
                }}
              >
                <Space direction="vertical" size="large">
                  <Spin 
                    indicator={<LoadingOutlined style={{ fontSize: 48, color: '#FFB7C5' }} spin />}
                  />
                  <div>
                    <Title level={4} style={{ margin: '0 0 8px 0', color: '#FFB7C5' }}>
                      AI가 당신의 오시를 분석하고 있습니다
                    </Title>
                    <Text style={{ opacity: 0.7 }}>
                      분석이 완료되면 자동으로 결과가 표시됩니다
                    </Text>
                  </div>
                  <Progress 
                    percent={100}
                    strokeColor={{
                      '0%': '#FFB7C5',
                      '100%': '#FF85C0'
                    }}
                    status="active"
                    showInfo={false}
                    style={{ width: '300px' }}
                  />
                </Space>
              </Card>
            ) : lastResult && lastResult.recommendations && lastResult.recommendations.length > 0 ? (
              <>
                <div style={{ textAlign: 'center', marginBottom: '24px' }}>
                  <HeartOutlined style={{ fontSize: '16px', color: '#FFB7C5', marginRight: '8px' }} />
                  <Text style={{ fontSize: '16px', fontWeight: 500, opacity: 0.8 }}>
                    나의 오시 매칭 결과
                  </Text>
                </div>
                
                <Card 
                  className="glass-effect hover-float"
                  variant="borderless"
                  style={{ 
                    background: isDarkMode 
                      ? 'linear-gradient(135deg, rgba(255,183,197,0.08), rgba(255,133,192,0.05))' 
                      : 'linear-gradient(135deg, rgba(255,245,247,0.95), rgba(255,255,255,0.9))',
                    borderRadius: '16px',
                    border: '1px solid rgba(255,183,197,0.2)',
                    cursor: 'pointer'
                  }}
                  styles={{ 
                    body: { padding: '20px 24px' }
                  }}
                  onClick={() => navigate('/result', { state: { result: lastResult } })}
                >
                  <Row align="middle" justify="space-between">
                    <Col>
                      <Space align="center" size={16}>
                        <Avatar 
                          size={48}
                          style={{ 
                            background: 'linear-gradient(135deg, #FFB7C5, #FF85C0)',
                            fontSize: '20px',
                            fontWeight: 'bold'
                          }}
                        >
                          {lastResult.recommendations[0].name.charAt(0)}
                        </Avatar>
                        <div>
                          <Title level={5} style={{ margin: 0, fontSize: '16px' }}>
                            {lastResult.recommendations[0].name}
                          </Title>
                          <Text style={{ fontSize: '13px', opacity: 0.6 }}>
                            매칭도 {Math.round(lastResult.recommendations[0].matchScore * 10)}%
                          </Text>
                        </div>
                      </Space>
                    </Col>
                    
                    <Col>
                      <Button 
                        type="text"
                        style={{ color: '#FFB7C5' }}
                      >
                        결과 보기 →
                      </Button>
                    </Col>
                  </Row>
                </Card>
              </>
            ) : (
              <div style={{ textAlign: 'center', opacity: 0.5, padding: '40px 0' }}>
                <HeartOutlined style={{ fontSize: '24px', color: '#FFB7C5', marginBottom: '12px', display: 'block' }} />
                <Text style={{ fontSize: '14px' }}>
                  아직 매칭 결과가 없습니다
                </Text>
              </div>
            )}
          </div>

        </div>
      </main>

      {/* Footer */}
      <footer style={{ 
        textAlign: 'center', 
        padding: '24px', 
        opacity: 0.6,
        borderTop: '1px solid rgba(255, 255, 255, 0.1)'
      }}>
        <Paragraph style={{ margin: 0, fontSize: '14px' }}>
          © 2025 Holo-Oshi Finder | Fan-made with love
        </Paragraph>
      </footer>
    </div>
  )
}

export default HomePage