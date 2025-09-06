export interface QuizOption {
  id: string;
  label: string;
  value: string;
  traits: string[];
}

export interface QuizQuestion {
  id: number;
  question: string;
  type: 'single' | 'multiple';
  options?: QuizOption[];
  category: string;
  weight: number;
}

export interface QuizAnswer {
  questionId: number;
  answer: string | string[] | number;
}

export interface SurveyResponse {
  questionId: string;
  question: string;
  answer: string;
  importance?: number;
}

export interface MemberRecommendation {
  memberId: string;
  name: string;
  matchScore: number;
  matchingTraits?: string[];
  reasoning: string;
  strengths?: string[];
  considerations?: string[];
}

export interface AnalysisResponse {
  recommendations: MemberRecommendation[];
  overallAnalysis: string;
  userProfile: {
    preferredTraits: string[];
    personalityMatch: string;
  };
  confidence: number;
  processingTime: number;
  fromCache?: boolean;
  thinkingProcess?: string;
}