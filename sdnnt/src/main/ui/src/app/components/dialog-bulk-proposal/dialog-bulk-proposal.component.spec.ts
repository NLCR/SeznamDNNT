import { ComponentFixture, TestBed } from '@angular/core/testing';

import { DialogBulkProposalComponent } from './dialog-bulk-proposal.component';

describe('DialogBulkProposalComponent', () => {
  let component: DialogBulkProposalComponent;
  let fixture: ComponentFixture<DialogBulkProposalComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ DialogBulkProposalComponent ]
    })
    .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(DialogBulkProposalComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
