const fs = require('node:fs');
const path = require('node:path');

const lab2Root = path.resolve(__dirname, '..');
const sourcePath = path.resolve(lab2Root, '../lab-1/openapi.bundle.json');
const specsDir = path.join(lab2Root, 'specs');

const source = JSON.parse(fs.readFileSync(sourcePath, 'utf8'));

function copy(value) {
  return JSON.parse(JSON.stringify(value));
}

function selectPaths(predicate) {
  return Object.fromEntries(
    Object.entries(source.paths).filter(([resourcePath]) => predicate(resourcePath)).map(copy)
  );
}

const ticketService = copy(source);
ticketService.info = {
  ...ticketService.info,
  title: 'Ticket Service API'
};
ticketService.tags = (ticketService.tags || []).filter(tag =>
  ['Tickets', 'Ticket queries'].includes(tag.name)
);
ticketService.paths = selectPaths(resourcePath => resourcePath.startsWith('/tickets'));

const eventInput = copy(ticketService.components.schemas.Event);
eventInput.title = 'Данные мероприятия для запроса';
delete eventInput.properties.id;
eventInput.required = (eventInput.required || []).filter(field => field !== 'id');

const ticketInput = copy(ticketService.components.schemas.Ticket);
ticketInput.title = 'Данные билета для создания или полной замены';
delete ticketInput.properties.id;
delete ticketInput.properties.creationDate;
ticketInput.required = (ticketInput.required || []).filter(
  field => field !== 'id' && field !== 'creationDate'
);
ticketInput.properties.event = { $ref: '#/components/schemas/EventInput' };

ticketService.components.schemas.EventInput = eventInput;
ticketService.components.schemas.TicketInput = ticketInput;
ticketService.components.schemas.TicketPatch.properties.event = {
  $ref: '#/components/schemas/EventInput'
};
ticketService.components.parameters.ticket.content['application/json'].schema = {
  $ref: '#/components/schemas/TicketInput'
};

const bookingService = copy(source);
bookingService.info = {
  ...bookingService.info,
  title: 'Booking Service API'
};
bookingService.servers = [{ url: '/booking', description: 'Booking service' }];
bookingService.tags = (bookingService.tags || []).filter(tag => tag.name === 'Booking');
bookingService.paths = selectPaths(resourcePath =>
  resourcePath.startsWith('/sell/') || resourcePath.startsWith('/person/')
);
for (const pathItem of Object.values(bookingService.paths)) {
  delete pathItem.servers;
}
bookingService.components = {
  schemas: {
    ErrorResponse: copy(source.components.schemas.ErrorResponse)
  }
};

fs.mkdirSync(specsDir, { recursive: true });
fs.writeFileSync(
  path.join(specsDir, 'ticket-service.openapi.json'),
  `${JSON.stringify(ticketService, null, 2)}\n`
);
fs.writeFileSync(
  path.join(specsDir, 'booking-service.openapi.json'),
  `${JSON.stringify(bookingService, null, 2)}\n`
);

console.log(`Ticket service: ${Object.keys(ticketService.paths).length} paths`);
console.log(`Booking service: ${Object.keys(bookingService.paths).length} paths`);
