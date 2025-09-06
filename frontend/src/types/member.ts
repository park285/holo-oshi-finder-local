export interface Member {
  id: string;
  name: string;
  name_en?: string;
  name_ja?: string;
  branch: 'JP' | 'EN' | 'ID';
  generation?: string;
  debut_date?: string;
  status: 'active' | 'graduated' | 'inactive';
  traits: string[];
  description?: string;
  image_url?: string;
  youtube_channel?: string;
  twitter?: string;
}

export interface MemberRecommendation {
  memberId: string;
  name: string;
  matchScore: number;
  matchingTraits: string[];
  reasoning: string;
  confidence: number;
}

export interface QuizQuestion {
  id: number;
  question: string;
  type: 'single' | 'multiple' | 'slider' | 'text';
  category?: string;
  weight?: number;
  options?: Array<{
    id: string;
    label: string;
    value: string;
    traits?: string[];
  }>;
  required?: boolean;
}

export interface AnalysisResponse {
  analysis: {
    personalityProfile: string;
    preferences: string[];
    keywords: string[];
  };
  recommendations: MemberRecommendation[];
  overallAnalysis: string;
  userProfile: {
    preferredTraits: string[];
    personalityMatch: string;
  };
  confidence: number;
  analysisConfidence?: number;
  processingTime: number;
  processingInfo: {
    ragUsed: boolean;
    totalCandidates: number;
    processingTimeMs: number;
    modelUsed: string;
  };
  fromCache?: boolean;
}