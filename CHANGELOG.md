# Changelog
All notable changes to this project will be documented in this file.

## [8.0.0] - 2019-02-28
### Added
- Multilinguality support
- Updated data model

### Changed
- Improved access policies for company settings

## [7.0.0] - 2019-02-04
### Added
- Dedicated database for binary content
- Endpoint for verified companies
- Flag for including roles of users in responses
- Images downsized before storing them in the database

### Changed
- Optimized database connection pool settings
- Authentication error during company registration
- Homogenous endoints for managing company settings

## [6.0.0] - 2018-12-21
### Added
- New admin endpoints
    list of unverified companies
    verification of company
    list of registered companies
    deletion of companies.
- Endpoint for listing all company members
- Configurable SMTP settings

### Changed
- Updated calculation of the profile completeness to be compatible with the frontend
- Fix for storing large binary files
- Fix for dropping database connections
- Fix for dropping database connections
- Fix for dropping database connections with validation query


## [4.0.0] - 2018-09-14
### Added

### Changed
- Fixed issue in reset password mechanism

## [3.0.0] - 2018-06-01
### Added
- Register on platform

 ---
The project leading to this application has received funding from the European Union’s Horizon 2020 research and innovation programme under grant agreement No 723810.
