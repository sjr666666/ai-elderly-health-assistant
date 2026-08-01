import React from 'react';
import DrugListView from './DrugListView';

function DrugManagementTab({
  drugList,
  searchQuery,
  isSearching,
  filteredDrugList,
  calendarPlans,
  onSearch,
  onAddDrug,
  onOpenDrugDetail,
  onOpenAddToPlanModal,
  onDiscardDrug,
  onDeleteDrug,
  onReloadDrugList,
  user
}) {
  return (
    <DrugListView
      drugList={drugList}
      searchQuery={searchQuery}
      isSearching={isSearching}
      filteredDrugList={filteredDrugList}
      calendarPlans={calendarPlans}
      onSearch={onSearch}
      onAddDrug={onAddDrug}
      onOpenDrugDetail={onOpenDrugDetail}
      onOpenAddToPlanModal={onOpenAddToPlanModal}
      onDiscardDrug={onDiscardDrug}
      onDeleteDrug={onDeleteDrug}
      onReloadDrugList={onReloadDrugList}
      user={user}
    />
  );
}

export default DrugManagementTab;
