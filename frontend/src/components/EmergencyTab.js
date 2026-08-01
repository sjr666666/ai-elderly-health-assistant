import React from 'react';
import EmergencyAssistant from './EmergencyAssistant';

function EmergencyTab({ emergencyContacts, elderId }) {
  return (
    <div className="card emergency-card">
      <EmergencyAssistant emergencyContacts={emergencyContacts} elderId={elderId} />
    </div>
  );
}

export default EmergencyTab;
