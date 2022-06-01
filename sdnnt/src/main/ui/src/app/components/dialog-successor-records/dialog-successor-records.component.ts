import { toBase64String } from '@angular/compiler/src/output/source_map';
import { Component, OnInit } from '@angular/core';

@Component({
  selector: 'app-dialog-successor-records',
  templateUrl: './dialog-successor-records.component.html',
  styleUrls: ['./dialog-successor-records.component.scss']
})
export class DialogSuccessorRecordsComponent implements OnInit {

  data = [
    {expanded: false, title: 'Muzeum hlavního města Prahy = Museum der Hauptstadt Prag = Museum of Prague / Jarmila Jindrová ; [souběž. překlad textu z češ. do něm. Antonín Rykl, [do angl.] Joy Turner-Kadečková ; Snímky Soňa Divišová]', desc: "blabla", url: 'http://inovatika.cz'},
    {expanded: false, title: 'Povolání pro chlapce / Autoři: Oldřich Šandera, Jaroslav Kraus ; Fot.: Bohumil Havránek', desc: "blabla", url: 'http://inovatika.cz'},
    {expanded: false, title: 'Revír bez hranic / [Text a fot.:] Rudolf Luskač', desc: "blabla", url: 'http://inovatika.cz'},
    {expanded: false, title: 'Povolání pro chlapce / Autoři: Oldřich Šandera, Jaroslav Kraus ; Fot.: Bohumil Havránek', desc: "blabla", url: 'http://inovatika.cz'}
  ];

  constructor() { }

  ngOnInit(): void {
  }

  accept() {
    // to do
  }

}
