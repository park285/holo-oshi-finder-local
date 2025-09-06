import { useEffect } from 'react'
import { useLocation, useNavigate } from 'react-router-dom'
import { Button, Card, Typography, Tag, Progress, Space, Divider, Avatar, Row, Col, Empty, List } from 'antd'
import { HomeOutlined, ReloadOutlined, StarFilled, TeamOutlined, UserOutlined } from '@ant-design/icons'
import type { AnalysisResponse, MemberRecommendation } from '../types'

const { Title, Text, Paragraph } = Typography

function ResultPage() {
  const location = useLocation()
  const navigate = useNavigate()
  let result = location.state?.result as AnalysisResponse

  // location.state가 없으면 localStorage에서 가져오기
  if (!result) {
    const savedResult = localStorage.getItem('lastMatchResult')
    if (savedResult) {
      try {
        result = JSON.parse(savedResult)
      } catch (e) {
        console.error('Failed to parse saved result:', e)
      }
    }
  }

  // 결과를 localStorage에 저장 (location.state에서 온 경우에만)
  useEffect(() => {
    if (result && location.state?.result) {
      const savedResult = {
        ...result,
        timestamp: new Date().toISOString()
      }
      localStorage.setItem('lastMatchResult', JSON.stringify(savedResult))
    }
  }, [result, location.state])

  if (!result) {
    // 결과가 전혀 없는 경우
    const savedResult = localStorage.getItem('lastMatchResult')
    if (savedResult) {
      const parsed = JSON.parse(savedResult)
      return (
        <div className="min-h-screen flex items-center justify-center">
          <Empty
            description={
              <Space direction="vertical" size="large">
                <Title level={3}>현재 결과를 찾을 수 없습니다</Title>
                <Text>이전 매칭 결과를 보시려면 홈으로 이동하세요</Text>
                <Button 
                  type="primary" 
                  icon={<HomeOutlined />}
                  onClick={() => navigate('/')}
                  size="large"
                >
                  홈으로 돌아가기
                </Button>
              </Space>
            }
          />
        </div>
      )
    }
    
    return (
      <div className="min-h-screen flex items-center justify-center">
        <Empty
          description={
            <Space direction="vertical" size="large">
              <Title level={3}>결과를 찾을 수 없습니다</Title>
              <Button 
                type="primary" 
                icon={<HomeOutlined />}
                onClick={() => navigate('/')}
                size="large"
              >
                홈으로 돌아가기
              </Button>
            </Space>
          }
        />
      </div>
    )
  }

  const topRecommendation = result.recommendations[0]

  return (
    <div className="min-h-screen" style={{ padding: '32px' }}>
      <div style={{ maxWidth: '1200px', margin: '0 auto' }}>
        {/* Header */}
        <div style={{ 
          display: 'flex', 
          justifyContent: 'space-between', 
          alignItems: 'center', 
          marginBottom: '32px' 
        }}>
          <Title level={2} style={{ margin: 0 }}>
            당신의 오시 분석 결과
          </Title>
          <Space>
            <Button 
              icon={<HomeOutlined />}
              onClick={() => navigate('/')}
              size="large"
            >
              홈으로
            </Button>
            <Button 
              type="primary"
              icon={<ReloadOutlined />}
              onClick={() => navigate('/quiz')}
              size="large"
            >
              다시 테스트
            </Button>
          </Space>
        </div>

        {/* Main Result Card */}
        {topRecommendation && (
          <Card 
            className="glass-effect"
            style={{ 
              background: 'linear-gradient(135deg, rgba(39, 199, 254, 0.1), rgba(139, 92, 246, 0.1))',
              marginBottom: '32px'
            }}
            variant="borderless"
          >
            <Row gutter={32} align="middle">
              <Col xs={24} md={6} style={{ textAlign: 'center' }}>
                <Avatar 
                  size={120} 
                  style={{ 
                    background: 'linear-gradient(135deg, #27C7FE, #8B5CF6)',
                    fontSize: '48px',
                    fontWeight: 'bold'
                  }}
                  icon={<UserOutlined />}
                >
                  {topRecommendation.name.charAt(0)}
                </Avatar>
              </Col>
              
              <Col xs={24} md={18}>
                <Title level={2} style={{ marginBottom: '16px' }}>
                  {topRecommendation.name}
                </Title>
                <div style={{ marginBottom: '16px' }}>
                  <Progress 
                    percent={Math.round(topRecommendation.matchScore * 10)}
                    strokeColor={{
                      '0%': '#27C7FE',
                      '100%': '#8B5CF6'
                    }}
                    size="default"
                    format={percent => `매칭도 ${percent}%`}
                  />
                </div>
                <Paragraph style={{ fontSize: '16px', marginBottom: '16px' }}>
                  {topRecommendation.reasoning}
                </Paragraph>
                
                {topRecommendation.matchingTraits && (
                  <Space wrap>
                    {topRecommendation.matchingTraits.slice(0, 5).map((trait, index) => (
                      <Tag 
                        key={index}
                        color="cyan"
                        style={{ padding: '4px 12px', fontSize: '14px' }}
                      >
                        {trait}
                      </Tag>
                    ))}
                  </Space>
                )}
              </Col>
            </Row>

            {/* Strengths and Content Recommendations */}
            <Divider style={{ margin: '24px 0' }} />
            
            <Row gutter={32}>
              {topRecommendation.strengths && topRecommendation.strengths.length > 0 && (
                <Col xs={24} md={12}>
                  <Title level={4} style={{ marginBottom: '16px' }}>
                    <StarFilled style={{ color: '#FAAD14', marginRight: '8px' }} />
                    강점
                  </Title>
                  <List
                    dataSource={topRecommendation.strengths}
                    renderItem={item => (
                      <List.Item style={{ border: 'none', padding: '8px 0' }}>
                        <Text>• {item}</Text>
                      </List.Item>
                    )}
                  />
                </Col>
              )}
              
              {topRecommendation.contentRecommendations && topRecommendation.contentRecommendations.length > 0 && (
                <Col xs={24} md={12}>
                  <Title level={4} style={{ marginBottom: '16px' }}>
                    <TeamOutlined style={{ color: '#8B5CF6', marginRight: '8px' }} />
                    추천 콘텐츠
                  </Title>
                  <List
                    dataSource={topRecommendation.contentRecommendations}
                    renderItem={item => (
                      <List.Item style={{ border: 'none', padding: '8px 0' }}>
                        <Text>• {item}</Text>
                      </List.Item>
                    )}
                  />
                </Col>
              )}
            </Row>
          </Card>
        )}

        {/* Other Recommendations */}
        {result.recommendations.length > 1 && (
          <>
            <Title level={3} style={{ marginBottom: '24px' }}>
              다른 추천 멤버들
            </Title>
            <Row gutter={[16, 16]}>
              {result.recommendations.slice(1, 7).map((member, index) => (
                <Col xs={24} md={12} lg={8} key={index}>
                  <Card 
                    className="glass-effect hover-float"
                    variant="borderless"
                  >
                    <Space align="start">
                      <Avatar 
                        size={48}
                        style={{ 
                          background: 'linear-gradient(135deg, #27C7FE, #8B5CF6)',
                          flexShrink: 0
                        }}
                      >
                        {member.name.charAt(0)}
                      </Avatar>
                      <div style={{ flex: 1 }}>
                        <Title level={5} style={{ marginBottom: '8px' }}>
                          {member.name}
                        </Title>
                        <Text type="secondary" style={{ color: '#27C7FE' }}>
                          매칭도 {Math.round(member.matchScore * 10)}%
                        </Text>
                        <Paragraph 
                          ellipsis={{ rows: 2 }} 
                          style={{ marginTop: '8px', marginBottom: '8px' }}
                        >
                          {member.reasoning}
                        </Paragraph>
                        {member.matchingTraits && (
                          <Space wrap size="small">
                            {member.matchingTraits.slice(0, 3).map((trait, idx) => (
                              <Tag key={idx} color="blue" style={{ fontSize: '12px' }}>
                                {trait}
                              </Tag>
                            ))}
                          </Space>
                        )}
                      </div>
                    </Space>
                  </Card>
                </Col>
              ))}
            </Row>
          </>
        )}

        {/* User Profile Analysis */}
        {result.userProfile && (
          <Card 
            className="glass-effect"
            variant="borderless"
            style={{ marginTop: '32px' }}
          >
            <Title level={3} style={{ marginBottom: '24px' }}>
              당신의 취향 분석
            </Title>
            
            <Row gutter={32}>
              {result.userProfile.preferredTraits && (
                <Col xs={24} md={8}>
                  <Title level={5} style={{ color: '#27C7FE', marginBottom: '12px' }}>
                    선호하는 특성
                  </Title>
                  <Space wrap>
                    {result.userProfile.preferredTraits.map((trait, index) => (
                      <Tag key={index} color="cyan">
                        {trait}
                      </Tag>
                    ))}
                  </Space>
                </Col>
              )}
              
              
              {result.userProfile.personalityMatch && (
                <Col xs={24} md={8}>
                  <Title level={5} style={{ color: '#52C41A', marginBottom: '12px' }}>
                    성격 매칭
                  </Title>
                  <Paragraph>
                    {result.userProfile.personalityMatch}
                  </Paragraph>
                </Col>
              )}
            </Row>
          </Card>
        )}

        {/* Overall Analysis */}
        {result.overallAnalysis && (
          <Card 
            className="glass-effect"
            variant="borderless"
            style={{ marginTop: '32px' }}
          >
            <Title level={4} style={{ marginBottom: '16px' }}>
              종합 분석
            </Title>
            <Paragraph style={{ whiteSpace: 'pre-line', fontSize: '15px' }}>
              {result.overallAnalysis}
            </Paragraph>
          </Card>
        )}

        {/* Footer Info */}
        <div style={{ textAlign: 'center', marginTop: '32px', opacity: 0.6 }}>
          <Text type="secondary">
            분석 신뢰도: {result.confidence ? Math.round(result.confidence * 100) : (result.analysisConfidence ? Math.round(result.analysisConfidence * 100) : 95)}% | 
            처리 시간: {result.processingTime || 1000}ms
            {result.fromCache && ' (캐시됨)'}
          </Text>
        </div>
      </div>
    </div>
  )
}

export default ResultPage