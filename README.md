# Tecnologie Informatiche per il Web

## Image Gallery
This project, developed as part of the **Web Information Technologies** course for the academic year 2023-2024, aims to create a web application dedicated to managing and organizing an image gallery.  
It is divided into two versions, organized through Git branches:  
- **Pure HTML**: basic version with features implemented using only HTML and CSS
- **RIA (Rich Internet Application)**: advanced version with dynamic features and asynchronous interactions powered by JavaScript  

[Requirements](https://github.com/filo3r/tiw-2024-denicola-raimondi/tree/RIA/documents/project-specification.pdf)


### Pure HTML
**Branch**: [pure-HTML](https://github.com/filo3r/tiw-2024-denicola-raimondi/tree/pure-HTML)  
The **Pure HTML** version implements:  
- Registration and login with basic server-side validations  
- Management of albums and images with metadata stored in the database  
- Display of albums and images sorted by date  
- Navigation between pages (home, album, image)  
- Adding comments and deleting images for the owner  

[Documentation](https://github.com/filo3r/tiw-2024-denicola-raimondi/tree/pure-HTML/documents/project-documentation-pureHTML.pdf)


### RIA
**Branch**: [RIA](https://github.com/filo3r/tiw-2024-denicola-raimondi/tree/RIA)  
The **RIA** version implements:  
- Single-page application with asynchronous interactions  
- Client-side validations (user data, non-empty comments)  
- Dynamic display with modal windows for image details  
- Custom reordering of images via drag-and-drop  
- Server error handling with in-page notifications  

[Documentation](https://github.com/filo3r/tiw-2024-denicola-raimondi/tree/RIA/documents/project-documentation-RIA.pdf)  


## Installation and Use
### Prerequisites
- Git to clone the repository and manage branches
- Local server (e.g. Apache Tomcat)
- Modern browser (Chrome, Firefox, or similar)

### Installation
1. Clone the repository:  
```bash
git clone https://github.com/filo3r/tiw-2024-denicola-raimondi.git
``` 
2. Switch to the desired branch:  
```bash
git checkout pure-HTML
```
```bash
git checkout RIA
```  
### Configuration
1. Import database schema:  
   - pure HTML version: use the [database_structure.sql](https://github.com/filo3r/tiw-2024-denicola-raimondi/blob/pure-HTML/src/main/resources/database/database_structure.sql) file  
   - RIA version: use the [database_structure.sql](https://github.com/filo3r/tiw-2024-denicola-raimondi/blob/RIA/src/main/resources/database/database_structure.sql) file  
2. Set database credentials:
   - pure HTML version: use the [database.properties](https://github.com/filo3r/tiw-2024-denicola-raimondi/blob/pure-HTML/src/main/resources/properties/database.properties) file  
   - RIA version: use the [database.properties](https://github.com/filo3r/tiw-2024-denicola-raimondi/blob/RIA/src/main/resources/properties/database.properties)  file  
3. Set the directory to save uploaded images:  
   - pure HTML version: use the [uploads.properties](https://github.com/filo3r/tiw-2024-denicola-raimondi/blob/pure-HTML/src/main/resources/properties/uploads.properties) file  
   - RIA version: use the [uploads.properties](https://github.com/filo3r/tiw-2024-denicola-raimondi/blob/RIA/src/main/resources/properties/uploads.properties) file  


## Technologies Used  
- Java  
- HTML
- CSS
- JavaScript 
- Servlet API
- Thymeleaf
- Maven
- MySQL
- JBCrypt
- JUnit
- SLF4J
- Gson
