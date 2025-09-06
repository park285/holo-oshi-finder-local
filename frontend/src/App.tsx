import { BrowserRouter as Router, Routes, Route } from 'react-router-dom'
import { ConfigProvider, theme } from 'antd'
import { useState, useEffect } from 'react'
import HomePage from './pages/HomePage'
import QuizPage from './pages/QuizPage'
import ResultPage from './pages/ResultPage'

function App() {
  const [isDarkMode, setIsDarkMode] = useState(() => {
    const savedTheme = localStorage.getItem('theme')
    return savedTheme ? savedTheme === 'dark' : true
  })

  useEffect(() => {
    localStorage.setItem('theme', isDarkMode ? 'dark' : 'light')
  }, [isDarkMode])

  const antdTheme = {
    algorithm: isDarkMode ? theme.darkAlgorithm : theme.defaultAlgorithm,
    token: {
      colorPrimary: '#FFB7C5',      // Cherry Blossom Pink
      colorSuccess: '#95DE64',      // Spring Green  
      colorWarning: '#FFD666',      // Sunshine Yellow
      colorError: '#FF7875',        // Coral
      colorInfo: '#85A5FF',         // Sky Blue
      
      colorBgBase: isDarkMode ? '#1f1f1f' : '#fefefe',
      colorTextBase: isDarkMode ? '#f0f0f0' : '#262626',
            borderRadius: 8,
      borderRadiusLG: 12,
      borderRadiusSM: 4,
      
      fontSize: 14,
      fontSizeHeading1: 38,
      fontSizeHeading2: 30,
      fontSizeHeading3: 24,
      fontSizeHeading4: 20,
      fontSizeHeading5: 16,
      fontFamily: '"Pretendard", "Noto Sans JP", -apple-system, BlinkMacSystemFont, system-ui, sans-serif',
      
      lineWidth: 1,
      lineType: 'solid',
      
      marginXXS: 4,   // 0.5 * 8px
      marginXS: 8,    // 1 * 8px
      marginSM: 12,   // 1.5 * 8px
      marginMD: 16,   // 2 * 8px
      marginLG: 24,   // 3 * 8px
      marginXL: 32,   // 4 * 8px
      marginXXL: 48,  // 6 * 8px
      
      paddingXXS: 4,
      paddingXS: 8,
      paddingSM: 12,
      paddingMD: 16,
      paddingLG: 24,
      paddingXL: 32,
      paddingXXL: 48,
      
      motion: true,
      motionUnit: 0.1,
      motionBase: 0,
      motionDurationSlow: '0.3s',
      motionDurationMid: '0.2s',
      motionDurationFast: '0.1s',
      
      controlHeight: 36,
      controlHeightLG: 40,
      controlHeightSM: 32,
      
      boxShadow: '0 1px 2px 0 rgba(0, 0, 0, 0.03), 0 1px 6px -1px rgba(0, 0, 0, 0.02), 0 2px 4px 0 rgba(0, 0, 0, 0.02)',
      boxShadowSecondary: '0 6px 16px 0 rgba(0, 0, 0, 0.08), 0 3px 6px -4px rgba(0, 0, 0, 0.12), 0 9px 28px 8px rgba(0, 0, 0, 0.05)',
    },
    components: {
      Button: {
        colorPrimary: '#FFB7C5',
        algorithm: true,
        primaryShadow: '0 2px 0 rgba(255, 183, 197, 0.1)',
        primaryColor: '#FFB7C5',
        defaultBorderColor: 'rgba(255, 183, 197, 0.3)',
        defaultShadow: '0 2px 0 rgba(0, 0, 0, 0.015)',
        fontWeight: 500,
        contentFontSizeLG: 16,
      },
      Card: {
        paddingLG: 24,
        borderRadiusLG: 12,
        boxShadow: '0 1px 2px 0 rgba(0, 0, 0, 0.03), 0 1px 6px -1px rgba(0, 0, 0, 0.02), 0 2px 4px 0 rgba(0, 0, 0, 0.02)',
        boxShadowSecondary: isDarkMode 
          ? '0 6px 16px 0 rgba(0, 0, 0, 0.32), 0 3px 6px -4px rgba(0, 0, 0, 0.48), 0 9px 28px 8px rgba(0, 0, 0, 0.2)' 
          : '0 6px 16px 0 rgba(0, 0, 0, 0.08), 0 3px 6px -4px rgba(0, 0, 0, 0.12), 0 9px 28px 8px rgba(0, 0, 0, 0.05)',
      },
      Progress: {
        defaultColor: '#FFB7C5',
        remainingColor: 'rgba(255, 183, 197, 0.1)',
        circleTextFontSize: '1em',
      },
      Typography: {
        titleMarginTop: '1.2em',
        titleMarginBottom: '0.5em',
      },
      Tag: {
        defaultBg: 'rgba(255, 183, 197, 0.1)',
        defaultColor: '#d4556a',
      },
      Divider: {
        colorSplit: 'rgba(255, 183, 197, 0.12)',
        algorithm: true,
      },
      Switch: {
        colorPrimary: '#FFB7C5',
        colorPrimaryHover: '#ffc9d3',
      },
    },
  }

  return (
    <ConfigProvider theme={antdTheme}>
      <div className={isDarkMode ? 'dark' : 'light'} data-theme={isDarkMode ? 'dark' : 'light'}>
        <Router>
          <Routes>
            <Route path="/" element={<HomePage onThemeToggle={() => setIsDarkMode(!isDarkMode)} isDarkMode={isDarkMode} />} />
            <Route path="/quiz" element={<QuizPage />} />
            <Route path="/result" element={<ResultPage />} />
          </Routes>
        </Router>
      </div>
    </ConfigProvider>
  )
}

export default App
