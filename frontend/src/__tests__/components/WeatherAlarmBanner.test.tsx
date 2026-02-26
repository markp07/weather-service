/**
 * @jest-environment jsdom
 */
import React from 'react';
import { render, screen, fireEvent } from '@testing-library/react';

// Mock next-intl
jest.mock('next-intl', () => ({
  useTranslations: (namespace: string) => (key: string) => `${namespace}.${key}`,
}));

// Mock react-bootstrap-icons
jest.mock('react-bootstrap-icons', () => ({
  ExclamationTriangleFill: () => <svg data-testid="alarm-icon" />,
}));

import WeatherAlarmBanner from '../../components/WeatherAlarmBanner';

describe('WeatherAlarmBanner', () => {
  it('renders nothing when alarm is GREEN', () => {
    const { container } = render(<WeatherAlarmBanner alarm="GREEN" />);
    expect(container).toBeEmptyDOMElement();
  });

  it('renders nothing when alarm is empty', () => {
    const { container } = render(<WeatherAlarmBanner alarm="" />);
    expect(container).toBeEmptyDOMElement();
  });

  it('renders yellow alarm banner', () => {
    render(<WeatherAlarmBanner alarm="YELLOW" />);
    expect(screen.getByRole('alert')).toBeInTheDocument();
    expect(screen.getByText('alarm.yellow')).toBeInTheDocument();
  });

  it('renders orange alarm banner', () => {
    render(<WeatherAlarmBanner alarm="ORANGE" />);
    expect(screen.getByRole('alert')).toBeInTheDocument();
    expect(screen.getByText('alarm.orange')).toBeInTheDocument();
  });

  it('renders red alarm banner', () => {
    render(<WeatherAlarmBanner alarm="RED" />);
    expect(screen.getByRole('alert')).toBeInTheDocument();
    expect(screen.getByText('alarm.red')).toBeInTheDocument();
  });

  it('renders alarm icon', () => {
    render(<WeatherAlarmBanner alarm="YELLOW" />);
    expect(screen.getByTestId('alarm-icon')).toBeInTheDocument();
  });

  it('shows alarm types when alarmWarnings are provided', () => {
    const warnings = [
      { awarenessLevel: '3; orange; Severe', awarenessType: 'Wind' },
      { awarenessLevel: '3; orange; Severe', awarenessType: 'Thunderstorm' },
    ];
    render(<WeatherAlarmBanner alarm="ORANGE" alarmWarnings={warnings} />);
    expect(screen.getByText(/Wind/)).toBeInTheDocument();
    expect(screen.getByText(/Thunderstorm/)).toBeInTheDocument();
  });

  it('shows details label when onClick is provided', () => {
    render(<WeatherAlarmBanner alarm="YELLOW" onClick={() => {}} />);
    expect(screen.getByText('alarm.details')).toBeInTheDocument();
  });

  it('calls onClick when banner is clicked', () => {
    const onClick = jest.fn();
    render(<WeatherAlarmBanner alarm="YELLOW" onClick={onClick} />);
    fireEvent.click(screen.getByRole('alert'));
    expect(onClick).toHaveBeenCalledTimes(1);
  });
});
