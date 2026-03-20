/**
 * @jest-environment jsdom
 */
import React from 'react';
import { render, screen } from '@testing-library/react';
import Footer from '../../components/Footer';

describe('Footer Component', () => {
  it('should display the MIT License link', () => {
    render(<Footer />);
    const licenseLink = screen.getByText('MIT License');
    expect(licenseLink).toBeInTheDocument();
    expect(licenseLink).toHaveAttribute('href', 'https://github.com/markp07/weather-service/blob/main/LICENSE');
  });

  it('should display the current year', () => {
    render(<Footer />);
    const currentYear = new Date().getFullYear().toString();
    expect(screen.getByText(new RegExp(currentYear))).toBeInTheDocument();
  });

  it('should display the MarkPost.nl link', () => {
    render(<Footer />);
    const markPostLink = screen.getByText('Mark Post - MarkPost.nl');
    expect(markPostLink).toBeInTheDocument();
    expect(markPostLink).toHaveAttribute('href', 'https://www.markpost.nl');
  });

  it('should display the Open-Meteo data attribution link', () => {
    render(<Footer />);
    const openMeteoLink = screen.getByText('Open-Meteo');
    expect(openMeteoLink).toBeInTheDocument();
    expect(openMeteoLink).toHaveAttribute('href', 'https://open-meteo.com');
  });

  it('should display the BigDataCloud data attribution link', () => {
    render(<Footer />);
    const bigDataCloudLink = screen.getByText('BigDataCloud');
    expect(bigDataCloudLink).toBeInTheDocument();
    expect(bigDataCloudLink).toHaveAttribute('href', 'https://www.bigdatacloud.com');
  });

  it('should open external links in a new tab with noopener noreferrer', () => {
    render(<Footer />);
    const links = screen.getAllByRole('link');
    links.forEach((link) => {
      expect(link).toHaveAttribute('target', '_blank');
      expect(link).toHaveAttribute('rel', 'noopener noreferrer');
    });
  });
});
