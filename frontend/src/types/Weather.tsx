import {Current} from './Current';
import {Daily} from './Daily';
import {Hourly} from './Hourly';
import {AlarmWarning} from './AlarmWarning';

export interface Weather {
  latitude: number;
  longitude: number;
  location: string;
  timezone: string;
  elevation: number;
  current: Current;
  daily: Daily[];
  hourly: Hourly[];
  alarm?: string;
  alarmWarnings?: AlarmWarning[];
}

