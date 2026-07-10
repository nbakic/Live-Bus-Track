const defaults = {
  trip: {
    tripName: "Obiteljski Borneo 2026",
    destination: "Sabah · Kota Kinabalu",
    startDate: "",
    endDate: "",
    travelers: 3,
    currency: "EUR",
    budgetLimit: 6000,
    notes: "Putujemo nas troje: roditelji i kći od 16 godina. Glavni prioriteti: orangutani, priroda, dobar hotel i dovoljno odmora."
  },
  days: [
    {title:"Dolazak u Kota Kinabalu", text:"Transfer do hotela, lagana šetnja uz more, večera i rani odmor.", date:"Dan 1"},
    {title:"Grad i zalazak sunca", text:"Central Market, waterfront, lokalna hrana i Tanjung Aru predvečer.", date:"Dan 2"},
    {title:"Otočni izlet", text:"Brodom do otoka Sapi ili Manukan; snorkeling i povratak sredinom poslijepodneva.", date:"Dan 3"},
    {title:"Sepilok i Sandakan", text:"Rani let BKI–SDK, orangutani, Sun Bear Conservation Centre i večernji povratak.", date:"Dan 4"},
    {title:"Dan odmora", text:"Bazen, plaža, spa ili rezervni dan u slučaju lošijeg vremena.", date:"Dan 5"},
    {title:"Kinabalu Park", text:"Cjelodnevni privatni izlet prema Kundasangu i Kinabalu Parku.", date:"Dan 6"},
    {title:"Slobodan izbor", text:"Dodatni otočni izlet, rafting, kulturno selo ili lagani gradski dan.", date:"Dan 7"},
    {title:"Odlazak", text:"Check-out, zadnja kupnja i transfer prema zračnoj luci.", date:"Dan 8"}
  ],
  flights: [
    {route:"Seoul (ICN) → Kota Kinabalu (BKI)", date:"", ref:"Jin Air / upiši broj leta"},
    {route:"Kota Kinabalu (BKI) → Sandakan (SDK)", date:"", ref:"Jednodnevni izlet"},
    {route:"Sandakan (SDK) → Kota Kinabalu (BKI)", date:"", ref:"Večernji povratak"}
  ],
  hotels: [
    {name:"Hotel u Kota Kinabaluu", dates:"7 noći", ref:"Upiši rezervaciju"}
  ],
  expenses: [
    {name:"Međunarodni i regionalni letovi", category:"Prijevoz", amount:0},
    {name:"Hotel", category:"Smještaj", amount:0},
    {name:"Sepilok izlet", category:"Izleti", amount:0},
    {name:"Otočni izlet", category:"Izleti", amount:0},
    {name:"Hrana i piće", category:"Hrana", amount:0},
    {name:"Transferi i taksi", category:"Prijevoz", amount:0}
  ],
  checklist: [
    {text:"Putovnice važe najmanje 6 mjeseci", done:false},
    {text:"Putno zdravstveno osiguranje", done:false},
    {text:"Potvrde letova i hotela offline", done:false},
    {text:"Adapter za malezijske utičnice tip G", done:false},
    {text:"Sredstvo protiv komaraca", done:false},
    {text:"Krema za sunce i lagana kapa", done:false},
    {text:"Lagana kabanica ili sklopivi kišobran", done:false},
    {text:"Tanka odjeća dugih rukava", done:false},
    {text:"Mala vodootporna torba za otoke", done:false},
    {text:"Lijekovi i osnovna putna apoteka", done:false},
    {text:"eSIM ili lokalna SIM kartica", done:false},
    {text:"Provjeriti težinu prtljage na domaćim letovima", done:false}
  ]
};

let state = loadState();

function loadState(){
  try{
    const saved = localStorage.getItem("borneoPlanner");
    return saved ? JSON.parse(saved) : structuredClone(defaults);
  }catch(e){ return structuredClone(defaults); }
}
function saveState(){
  localStorage.setItem("borneoPlanner", JSON.stringify(state));
  updateStats();
}
function bindTrip(){
  ["tripName","destination","startDate","endDate","travelers","currency","budgetLimit","notes"].forEach(id=>{
    const el=document.getElementById(id);
    el.value=state.trip[id] ?? "";
    el.addEventListener("input",()=>{
      state.trip[id]= el.type==="number" ? Number(el.value) : el.value;
      saveState(); renderExpenses();
    });
  });
}
function esc(s=""){return String(s).replace(/[&<>"']/g,m=>({"&":"&amp;","<":"&lt;",">":"&gt;","\"":"&quot;","'":"&#039;"}[m]))}

function renderDays(){
  const list=document.getElementById("daysList");
  if(!state.days.length){list.innerHTML='<div class="empty">Nema dodanih dana.</div>';return}
  list.innerHTML=state.days.map((d,i)=>`
    <div class="day">
      <div class="day-badge">${i+1}<small>${esc(d.date||"Dan")}</small></div>
      <div>
        <input aria-label="Naslov dana" value="${esc(d.title)}" oninput="state.days[${i}].title=this.value;saveState()" style="font-weight:850;font-size:17px;margin-bottom:8px">
        <textarea aria-label="Opis dana" oninput="state.days[${i}].text=this.value;saveState()">${esc(d.text)}</textarea>
      </div>
      <div class="day-actions"><button class="icon-btn" onclick="moveDay(${i},-1)">↑</button><button class="icon-btn" onclick="moveDay(${i},1)">↓</button><button class="icon-btn" onclick="removeDay(${i})">✕</button></div>
    </div>`).join("");
}
function addDay(){state.days.push({title:"Novi dan",text:"Dodaj aktivnosti, vrijeme polaska i rezervacije.",date:`Dan ${state.days.length+1}`});saveState();renderDays()}
function removeDay(i){state.days.splice(i,1);saveState();renderDays()}
function moveDay(i,dir){const j=i+dir;if(j<0||j>=state.days.length)return;[state.days[i],state.days[j]]=[state.days[j],state.days[i]];saveState();renderDays()}

function renderFlights(){
  const list=document.getElementById("flightsList");
  list.innerHTML=state.flights.map((f,i)=>`
    <div class="flight-row">
      <div><label>Ruta</label><input value="${esc(f.route)}" oninput="state.flights[${i}].route=this.value;saveState()"></div>
      <div><label>Datum / vrijeme</label><input value="${esc(f.date)}" placeholder="npr. 21.7. 08:10" oninput="state.flights[${i}].date=this.value;saveState()"></div>
      <div><label>Let / rezervacija</label><input value="${esc(f.ref)}" oninput="state.flights[${i}].ref=this.value;saveState()"></div>
      <button class="btn btn-danger remove" onclick="state.flights.splice(${i},1);saveState();renderFlights()">✕</button>
    </div>`).join("") || '<div class="empty">Nema letova.</div>';
}
function addFlight(){state.flights.push({route:"",date:"",ref:""});saveState();renderFlights()}

function renderHotels(){
  const list=document.getElementById("hotelsList");
  list.innerHTML=state.hotels.map((h,i)=>`
    <div class="hotel-row">
      <div><label>Hotel</label><input value="${esc(h.name)}" oninput="state.hotels[${i}].name=this.value;saveState()"></div>
      <div><label>Datumi</label><input value="${esc(h.dates)}" oninput="state.hotels[${i}].dates=this.value;saveState()"></div>
      <div><label>Rezervacija</label><input value="${esc(h.ref)}" oninput="state.hotels[${i}].ref=this.value;saveState()"></div>
      <button class="btn btn-danger remove" onclick="state.hotels.splice(${i},1);saveState();renderHotels()">✕</button>
    </div>`).join("") || '<div class="empty">Nema hotela.</div>';
}
function addHotel(){state.hotels.push({name:"",dates:"",ref:""});saveState();renderHotels()}

function money(v){
  const cur=state.trip.currency||"EUR";
  try{return new Intl.NumberFormat("hr-HR",{style:"currency",currency:cur,maximumFractionDigits:0}).format(Number(v)||0)}
  catch(e){return `${Number(v)||0} ${cur}`}
}
function renderExpenses(){
  const list=document.getElementById("expensesList");
  list.innerHTML=state.expenses.map((e,i)=>`
    <div class="expense-row">
      <div><label>Trošak</label><input value="${esc(e.name)}" oninput="state.expenses[${i}].name=this.value;saveState()"></div>
      <div><label>Kategorija</label><select onchange="state.expenses[${i}].category=this.value;saveState()">
        ${["Prijevoz","Smještaj","Izleti","Hrana","Kupnja","Ostalo"].map(c=>`<option ${e.category===c?"selected":""}>${c}</option>`).join("")}
      </select></div>
      <div><label>Iznos</label><input type="number" min="0" value="${Number(e.amount)||0}" oninput="state.expenses[${i}].amount=Number(this.value);saveState();updateBudget()"></div>
      <button class="btn btn-danger remove" onclick="state.expenses.splice(${i},1);saveState();renderExpenses()">✕</button>
    </div>`).join("") || '<div class="empty">Nema troškova.</div>';
  updateBudget();
}
function addExpense(){state.expenses.push({name:"Novi trošak",category:"Ostalo",amount:0});saveState();renderExpenses()}
function updateBudget(){
  const total=state.expenses.reduce((s,e)=>s+(Number(e.amount)||0),0);
  const people=Math.max(1,Number(state.trip.travelers)||1);
  document.getElementById("budgetTotal").textContent=money(total);
  document.getElementById("budgetPerPerson").textContent=`${money(total/people)} po osobi`;
  const limit=Number(state.trip.budgetLimit)||0;
  document.getElementById("budgetProgress").style.width=(limit?Math.min(100,total/limit*100):0)+"%";
  document.getElementById("statBudget").textContent=money(total);
}
function renderChecklist(){
  const list=document.getElementById("checklist");
  list.innerHTML=state.checklist.map((c,i)=>`
    <label class="check ${c.done?"done":""}">
      <input type="checkbox" ${c.done?"checked":""} onchange="state.checklist[${i}].done=this.checked;saveState();renderChecklist()">
      <span contenteditable="true" onblur="state.checklist[${i}].text=this.textContent;saveState()">${esc(c.text)}</span>
      <button type="button" class="icon-btn" style="margin-left:auto" onclick="event.preventDefault();state.checklist.splice(${i},1);saveState();renderChecklist()">✕</button>
    </label>`).join("");
}
function addChecklistItem(){state.checklist.push({text:"Nova stavka",done:false});saveState();renderChecklist()}

function updateStats(){
  document.getElementById("statDestination").textContent=(state.trip.destination||"Sabah").split("·")[0].trim();
  document.getElementById("statTravelers").textContent=state.trip.travelers||0;
  let nights=7;
  if(state.trip.startDate && state.trip.endDate){
    const a=new Date(state.trip.startDate),b=new Date(state.trip.endDate);
    nights=Math.max(0,Math.round((b-a)/86400000));
  } else if(state.days.length>1) nights=state.days.length-1;
  document.getElementById("statNights").textContent=nights;
  updateBudget();
}
function exportData(){
  const blob=new Blob([JSON.stringify(state,null,2)],{type:"application/json"});
  const url=URL.createObjectURL(blob);
  const a=document.createElement("a");a.href=url;a.download="borneo-plan.json";a.click();URL.revokeObjectURL(url);
}
document.getElementById("importFile").addEventListener("change",e=>{
  const file=e.target.files[0];if(!file)return;
  const r=new FileReader();r.onload=()=>{
    try{state=JSON.parse(r.result);saveState();location.reload()}
    catch(err){alert("Datoteka nije valjan Borneo plan.")}
  };r.readAsText(file)
});
function resetAll(){
  if(confirm("Želiš li vratiti cijeli početni plan?")){
    state=structuredClone(defaults);saveState();location.reload();
  }
}

bindTrip();
renderDays();
renderFlights();
renderHotels();
renderExpenses();
renderChecklist();
updateStats();