# Holo-Oshi Finder Design System

## 🎨 Color Palette (Blossom Theme)

### Primary Colors
- **Cherry Blossom Pink**: #FFB7C5 (Primary)
- **Deep Blossom**: #FF85C0 (Primary Gradient)
- **Spring Green**: #95DE64 (Success)
- **Sunshine Yellow**: #FFD666 (Warning)
- **Sky Blue**: #85A5FF (Info)
- **Coral**: #FF7875 (Error)

### Gradients
```css
--blossom-gradient: linear-gradient(135deg, #FFB7C5, #FF85C0);
--success-gradient: linear-gradient(135deg, #95DE64, #52C41A);
```

## 📐 Spacing System (8px Grid)

### Spacing Scale
- `XXS`: 4px (0.5 × 8)
- `XS`: 8px (1 × 8)  
- `SM`: 12px (1.5 × 8)
- `MD`: 16px (2 × 8)
- `LG`: 24px (3 × 8)
- `XL`: 32px (4 × 8)
- `XXL`: 48px (6 × 8)

### Usage
- **Component Padding**: paddingLG (24px) for cards
- **Section Spacing**: marginXXL (48px) between major sections
- **Element Spacing**: marginMD (16px) between related elements
- **Inline Spacing**: marginXS (8px) for tight spacing

## 📝 Typography Hierarchy

### Font Family
```
"Pretendard", "Noto Sans JP", -apple-system, system-ui, sans-serif
```

### Type Scale
1. **H1 - Page Title**: 38px, weight 700
   - Usage: Main page hero title
   - Line Height: 1.23
   
2. **H2 - Section Title**: 30px, weight 600
   - Usage: Major section headers
   - Line Height: 1.35
   
3. **H3 - Card Title**: 24px, weight 600
   - Usage: Card headers, subsections
   - Line Height: 1.38
   
4. **H4 - Sub-heading**: 20px, weight 500
   - Usage: Minor headings
   - Line Height: 1.4
   
5. **H5 - Label**: 16px, weight 500
   - Usage: Form labels, small titles
   - Line Height: 1.5

6. **Body**: 14px, weight 400
   - Usage: Regular text content
   - Line Height: 1.57
   
7. **Caption**: 12px, weight 400
   - Usage: Helper text, timestamps
   - Line Height: 1.66

## 🎯 Component Tokens

### Button
- **Height**: 36px (default), 40px (large), 32px (small)
- **Border Radius**: 8px
- **Font Weight**: 500
- **Primary Shadow**: 0 2px 8px rgba(255, 183, 197, 0.25)

### Card
- **Padding**: 24px (large), 16px (default)
- **Border Radius**: 12px (large), 8px (default)
- **Shadow**: 
  - Light: 0 1px 2px rgba(0,0,0,0.03), 0 2px 4px rgba(0,0,0,0.02)
  - Dark: 0 6px 16px rgba(0,0,0,0.32)

### Progress
- **Height**: 8px (default)
- **Border Radius**: 4px
- **Colors**: Gradient from #FFB7C5 to #FF85C0

### Tag
- **Height**: 24px
- **Padding**: 0 8px
- **Border Radius**: 4px
- **Background**: rgba(255, 183, 197, 0.1)
- **Border**: 1px solid rgba(255, 183, 197, 0.3)

## 🌓 Theme Modes

### Light Mode
- **Background Base**: #fefefe
- **Text Base**: #262626
- **Card Background**: rgba(255, 255, 255, 0.95)
- **Border Color**: rgba(0, 0, 0, 0.06)

### Dark Mode  
- **Background Base**: #1f1f1f
- **Text Base**: #f0f0f0
- **Card Background**: rgba(255, 255, 255, 0.04)
- **Border Color**: rgba(255, 255, 255, 0.08)

## 📱 Responsive Breakpoints

- **xs**: < 576px (Mobile)
- **sm**: ≥ 576px (Large Mobile)
- **md**: ≥ 768px (Tablet)
- **lg**: ≥ 992px (Desktop)
- **xl**: ≥ 1200px (Large Desktop)
- **xxl**: ≥ 1600px (Wide Screen)

## ✨ Motion & Animation

### Timing Functions
- **Fast**: 0.1s ease-in-out
- **Mid**: 0.2s ease-in-out
- **Slow**: 0.3s ease-in-out

### Common Animations
```css
/* Hover Float */
.hover-float:hover {
  transform: translateY(-4px);
  transition: transform 0.3s ease;
}

/* Gradient Animation */
@keyframes gradient {
  0%, 100% { background-position: 0% 50%; }
  50% { background-position: 100% 50%; }
}
```

## 🎭 Glass Effect
```css
.glass-effect {
  background: rgba(255, 255, 255, 0.05);
  backdrop-filter: blur(10px);
  border: 1px solid rgba(255, 255, 255, 0.1);
}
```

## 📋 Component Usage Examples

### Primary Button
```tsx
<Button 
  type="primary"
  size="large"
  style={{
    background: 'linear-gradient(90deg, #FFB7C5, #FF85C0)',
    border: 'none',
    height: '52px'
  }}
>
  시작하기 →
</Button>
```

### Feature Card
```tsx
<Card 
  className="glass-effect hover-float"
  style={{ 
    borderRadius: '16px',
    background: isDarkMode 
      ? 'rgba(255,255,255,0.03)' 
      : 'rgba(255,255,255,0.9)'
  }}
  bodyStyle={{ padding: '24px' }}
>
  {/* Content */}
</Card>
```

### Progress Bar
```tsx
<Progress 
  percent={85}
  strokeColor={{
    '0%': '#FFB7C5',
    '100%': '#FF85C0'
  }}
/>
```

## 🔧 Best Practices

1. **Consistency**: Always use predefined tokens instead of hardcoded values
2. **Spacing**: Follow the 8px grid system strictly
3. **Typography**: Use the defined type scale for all text elements
4. **Colors**: Stick to the Blossom theme palette
5. **Shadows**: Use subtle shadows for depth, avoid harsh drops
6. **Animations**: Keep animations smooth and purposeful (0.2-0.3s)
7. **Glass Effects**: Use sparingly for emphasis on key components
8. **Mobile First**: Design for mobile, then enhance for larger screens