export interface DiffPatch {
  op: string;
  path: string;
  value: string;
};

export class HistoryItem {
  id: string;
  identifier: string;
  indextime: Date;
  type: string;
  user: string;
  changes: {
    forward_patch: DiffPatch[];
    backward_patch: DiffPatch[];
  };
  [prop: string]: any;
}

