#!/usr/bin/env node
'use strict';

const fs = require('fs');
const path = require('path');
const os = require('os');
const { spawnSync } = require('child_process');
const http = require('http');
const https = require('https');
const { URL } = require('url');

let PgClient = null;

const OCCURRENCE_HEADERS = [
  'id', 'rightsHolder', 'accessRights', 'references', 'institutionCode', 'basisOfRecord', 'dynamicProperties',
  'occurrenceID', 'catalogNumber', 'occurrenceRemarks', 'individualCount', 'sex', 'lifeStage',
  'otherCatalogNumbers', 'previousIdentifications', 'associatedTaxa', 'eventID', 'samplingProtocol',
  'year', 'month', 'day', 'habitat', 'eventRemarks', 'islandGroup', 'island', 'country', 'stateProvince',
  'county', 'locality', 'minimumElevationInMeters', 'maximumElevationInMeters', 'minimumDepthInMeters',
  'maximumDepthInMeters', 'verbatimLatitude', 'verbatimLongitude', 'decimalLatitude', 'decimalLongitude',
  'coordinateUncertaintyInMeters', 'identifiedBy', 'identificationRemarks',
  'identificationVerificationStatus', 'typeStatus', 'scientificName', 'kingdom', 'phylum', 'class', 'order',
  'family', 'genus', 'subgenus', 'specificEpithet', 'infraspecificEpithet', 'taxonRank', 'taxonRemarks'
];

const MULTIMEDIA_HEADERS = [
  'id', 'identifier', 'type', 'Owner', 'accessURI', 'format', 'furtherInformationURL'
];

const CORE_TERMS = [
  'http://purl.org/dc/terms/rightsHolder',
  'http://purl.org/dc/terms/accessRights',
  'http://purl.org/dc/terms/references',
  'http://rs.tdwg.org/dwc/terms/institutionCode',
  'http://rs.tdwg.org/dwc/terms/basisOfRecord',
  'http://rs.tdwg.org/dwc/terms/dynamicProperties',
  'http://rs.tdwg.org/dwc/terms/occurrenceID',
  'http://rs.tdwg.org/dwc/terms/catalogNumber',
  'http://rs.tdwg.org/dwc/terms/occurrenceRemarks',
  'http://rs.tdwg.org/dwc/terms/individualCount',
  'http://rs.tdwg.org/dwc/terms/sex',
  'http://rs.tdwg.org/dwc/terms/lifeStage',
  'http://rs.tdwg.org/dwc/terms/otherCatalogNumbers',
  'http://rs.tdwg.org/dwc/terms/previousIdentifications',
  'http://rs.tdwg.org/dwc/terms/associatedTaxa',
  'http://rs.tdwg.org/dwc/terms/eventID',
  'http://rs.tdwg.org/dwc/terms/samplingProtocol',
  'http://rs.tdwg.org/dwc/terms/year',
  'http://rs.tdwg.org/dwc/terms/month',
  'http://rs.tdwg.org/dwc/terms/day',
  'http://rs.tdwg.org/dwc/terms/habitat',
  'http://rs.tdwg.org/dwc/terms/eventRemarks',
  'http://rs.tdwg.org/dwc/terms/islandGroup',
  'http://rs.tdwg.org/dwc/terms/island',
  'http://rs.tdwg.org/dwc/terms/country',
  'http://rs.tdwg.org/dwc/terms/stateProvince',
  'http://rs.tdwg.org/dwc/terms/county',
  'http://rs.tdwg.org/dwc/terms/locality',
  'http://rs.tdwg.org/dwc/terms/minimumElevationInMeters',
  'http://rs.tdwg.org/dwc/terms/maximumElevationInMeters',
  'http://rs.tdwg.org/dwc/terms/minimumDepthInMeters',
  'http://rs.tdwg.org/dwc/terms/maximumDepthInMeters',
  'http://rs.tdwg.org/dwc/terms/verbatimLatitude',
  'http://rs.tdwg.org/dwc/terms/verbatimLongitude',
  'http://rs.tdwg.org/dwc/terms/decimalLatitude',
  'http://rs.tdwg.org/dwc/terms/decimalLongitude',
  'http://rs.tdwg.org/dwc/terms/coordinateUncertaintyInMeters',
  'http://rs.tdwg.org/dwc/terms/identifiedBy',
  'http://rs.tdwg.org/dwc/terms/identificationRemarks',
  'http://rs.tdwg.org/dwc/terms/identificationVerificationStatus',
  'http://rs.tdwg.org/dwc/terms/typeStatus',
  'http://rs.tdwg.org/dwc/terms/scientificName',
  'http://rs.tdwg.org/dwc/terms/kingdom',
  'http://rs.tdwg.org/dwc/terms/phylum',
  'http://rs.tdwg.org/dwc/terms/class',
  'http://rs.tdwg.org/dwc/terms/order',
  'http://rs.tdwg.org/dwc/terms/family',
  'http://rs.tdwg.org/dwc/terms/genus',
  'http://rs.tdwg.org/dwc/terms/subgenus',
  'http://rs.tdwg.org/dwc/terms/specificEpithet',
  'http://rs.tdwg.org/dwc/terms/infraspecificEpithet',
  'http://rs.tdwg.org/dwc/terms/taxonRank',
  'http://rs.tdwg.org/dwc/terms/taxonRemarks'
];

const EXTENSION_TERMS = [
  'http://purl.org/dc/terms/identifier',
  'http://purl.org/dc/elements/1.1/type',
  'http://ns.adobe.com/xap/1.0/rights/Owner',
  'http://rs.tdwg.org/ac/terms/accessURI',
  'http://purl.org/dc/elements/1.1/format',
  'http://rs.tdwg.org/ac/terms/furtherInformationURL'
];

const SAMPLE_ALIASES = {
  rightsHolder: ['rightsHolder', 'urn:rightsHolder'],
  accessRights: ['accessRights', 'urn:accessRights'],
  institutionCode: ['institutionCode', 'urn:institutionCode'],
  basisOfRecord: ['basisOfRecord', 'urn:basisOfRecord'],
  occurrenceID: ['occurrenceID', 'urn:occurrenceID'],
  catalogNumber: ['materialSampleID', 'urn:materialSampleID'],
  occurrenceRemarks: ['occurrenceRemarks', 'urn:occurrenceRemarks', 'sampleRemarks', 'urn:sampleRemarks'],
  individualCount: ['individualCount', 'urn:individualCount'],
  sex: ['sex', 'urn:sex'],
  lifeStage: ['lifeStage', 'urn:lifeStage'],
  otherCatalogNumbers: ['otherCatalogNumbers', 'urn:otherCatalogNumbers'],
  previousIdentifications: ['previousIdentifications', 'urn:previousIdentifications'],
  associatedTaxa: ['associatedTaxa', 'urn:associatedTaxa'],
  identifiedBy: ['identifiedBy', 'urn:identifiedBy'],
  identificationRemarks: ['identificationRemarks', 'urn:identificationRemarks'],
  identificationVerificationStatus: ['identificationVerificationStatus', 'urn:identificationVerificationStatus'],
  typeStatus: ['typeStatus', 'urn:typeStatus'],
  scientificName: ['scientificName', 'urn:scientificName'],
  kingdom: ['kingdom', 'urn:kingdom'],
  phylum: ['phylum', 'urn:phylum'],
  class: ['class', 'urn:class'],
  order: ['order', 'urn:order'],
  family: ['family', 'urn:family'],
  genus: ['genus', 'urn:genus'],
  subgenus: ['subgenus', 'urn:subgenus'],
  specificEpithet: ['specificEpithet', 'urn:specificEpithet', 'species', 'urn:species'],
  infraspecificEpithet: ['infraspecificEpithet', 'urn:infraspecificEpithet', 'subspecies', 'urn:subspecies'],
  taxonRank: ['taxonRank', 'urn:taxonRank'],
  taxonRemarks: ['taxonRemarks', 'urn:taxonRemarks']
};

const EVENT_ALIASES = {
  eventID: ['eventID', 'urn:eventID', 'materialEventID', 'urn:materialEventID'],
  samplingProtocol: ['samplingProtocol', 'urn:samplingProtocol'],
  year: ['year', 'urn:year', 'yearCollected', 'urn:yearCollected'],
  month: ['month', 'urn:month', 'monthCollected', 'urn:monthCollected'],
  day: ['day', 'urn:day', 'dayCollected', 'urn:dayCollected'],
  habitat: ['habitat', 'urn:habitat', 'microHabitat', 'urn:microHabitat'],
  eventRemarks: ['eventRemarks', 'urn:eventRemarks'],
  islandGroup: ['islandGroup', 'urn:islandGroup'],
  island: ['island', 'urn:island'],
  country: ['country', 'urn:country', 'countryOrOcean', 'urn:countryOrOcean'],
  stateProvince: ['stateProvince', 'urn:stateProvince', 'province', 'urn:province', 'state', 'urn:state'],
  county: ['county', 'urn:county', 'countyOrMunicipality', 'urn:countyOrMunicipality'],
  locality: ['locality', 'urn:locality'],
  minimumElevationInMeters: ['minimumElevationInMeters', 'urn:minimumElevationInMeters', 'minimumElevation', 'urn:minimumElevation'],
  maximumElevationInMeters: ['maximumElevationInMeters', 'urn:maximumElevationInMeters', 'maximumElevation', 'urn:maximumElevation'],
  minimumDepthInMeters: ['minimumDepthInMeters', 'urn:minimumDepthInMeters', 'minimumDepth', 'urn:minimumDepth'],
  maximumDepthInMeters: ['maximumDepthInMeters', 'urn:maximumDepthInMeters', 'maximumDepth', 'urn:maximumDepth'],
  verbatimLatitude: ['verbatimLatitude', 'urn:verbatimLatitude', 'lat', 'urn:lat'],
  verbatimLongitude: ['verbatimLongitude', 'urn:verbatimLongitude', 'long', 'urn:long'],
  decimalLatitude: ['decimalLatitude', 'urn:decimalLatitude'],
  decimalLongitude: ['decimalLongitude', 'urn:decimalLongitude'],
  coordinateUncertaintyInMeters: ['coordinateUncertaintyInMeters', 'urn:coordinateUncertaintyInMeters', 'maxErrorDistance', 'urn:maxErrorDistance']
};

function ensureDependencies() {
  if (!PgClient) {
    let pg;
    try {
      pg = require('pg');
    } catch (err) {
      throw new Error('Missing dependency: pg. Install with `npm install pg`.');
    }
    PgClient = pg.Client;
  }
}

function clean(value) {
  if (value === null || value === undefined) return '';
  const text = String(value).trim();
  if (text.toLowerCase() === 'null') return '';
  return text;
}

function parseKeyValueFile(filePath) {
  const values = {};
  if (!fs.existsSync(filePath)) return values;
  const text = fs.readFileSync(filePath, 'utf8');
  for (const raw of text.split(/\r?\n/)) {
    const line = raw.trim();
    if (!line || line.startsWith('#') || !line.includes('=')) continue;
    const idx = line.indexOf('=');
    const key = line.slice(0, idx).trim();
    let value = line.slice(idx + 1).trim();
    if (!key) continue;
    if ((value.startsWith('"') && value.endsWith('"')) || (value.startsWith("'") && value.endsWith("'"))) {
      value = value.slice(1, -1);
    }
    values[key] = value;
  }
  return values;
}

function parseJdbcProps(filePath) {
  const values = parseKeyValueFile(filePath);
  const jdbcUrl = clean(values.bcidUrl);
  const m = jdbcUrl.match(/^jdbc:postgresql:\/\/([^/:]+)(?::(\d+))?\/([^?]+)/);
  if (m) {
    values.DB_HOST = m[1];
    values.DB_PORT = m[2] || '5432';
    values.DB_NAME = m[3];
  }
  if (values.bcidUser) values.DB_USER = values.bcidUser;
  if (values.bcidPassword) values.DB_PASSWORD = values.bcidPassword;
  return values;
}

function mergedEnv(envFile, dbPropsFile) {
  return {
    ...process.env,
    ...parseJdbcProps(dbPropsFile),
    ...parseKeyValueFile(envFile)
  };
}

function required(env, name) {
  const val = clean(env[name]);
  if (!val) throw new Error(`Missing required setting: ${name}`);
  return val;
}

function getJsonObject(value) {
  if (!value) return {};
  if (typeof value === 'object') return value;
  if (typeof value === 'string') {
    const text = value.trim();
    if (!text) return {};
    try {
      const obj = JSON.parse(text);
      return typeof obj === 'object' && obj ? obj : {};
    } catch (_err) {
      return {};
    }
  }
  return {};
}

function firstNonEmpty(data, keys) {
  for (const key of keys) {
    const val = clean(data[key]);
    if (val) return val;
  }
  return '';
}

function formatDecimal(value) {
  const text = clean(value);
  if (!text) return '';
  const parsed = Number(text);
  if (Number.isNaN(parsed)) return text;
  return parsed.toFixed(6);
}

function toN2t(identifier) {
  const token = clean(identifier);
  if (!token) return '';
  if (token.startsWith('http://') || token.startsWith('https://')) return token;
  return `https://n2t.net/${token}`;
}

function inferFormat(uri) {
  let ext = '';
  try {
    ext = path.extname(new URL(uri).pathname).replace('.', '').toLowerCase();
  } catch (_err) {
    ext = path.extname(String(uri || '')).replace('.', '').toLowerCase();
  }
  if (ext === 'jpg') return 'jpeg';
  return ext;
}

function dynamicProperties(project) {
  const props = {};
  const localcontextsId = clean(project.localcontexts_id);
  if (localcontextsId) {
    props.local_contexts_project_id = `https://localcontextshub.org/projects/${localcontextsId}/`;
  }
  const permitGuid = clean(project.permit_guid);
  if (permitGuid) {
    props.access_and_benefits_sharing = permitGuid;
  }
  return Object.keys(props).length ? JSON.stringify(props) : '';
}

function buildMetaXml() {
  const lines = ['<archive xmlns="http://rs.tdwg.org/dwc/text/" metadata="eml.xml">'];
  lines.push('  <core encoding="UTF-8" fieldsTerminatedBy="\\t" linesTerminatedBy="\\n" fieldsEnclosedBy="" ignoreHeaderLines="1" rowType="http://rs.tdwg.org/dwc/terms/Occurrence">');
  lines.push('    <files>');
  lines.push('      <location>occurrence.txt</location>');
  lines.push('    </files>');
  lines.push('    <id index="0" />');
  CORE_TERMS.forEach((term, idx) => lines.push(`    <field index="${idx + 1}" term="${term}"/>`));
  lines.push('  </core>');

  lines.push('  <extension encoding="UTF-8" fieldsTerminatedBy="\\t" linesTerminatedBy="\\n" fieldsEnclosedBy="" ignoreHeaderLines="1" rowType="http://rs.tdwg.org/ac/terms/Multimedia">');
  lines.push('    <files>');
  lines.push('      <location>multimedia.txt</location>');
  lines.push('    </files>');
  lines.push('    <coreid index="0" />');
  EXTENSION_TERMS.forEach((term, idx) => lines.push(`    <field index="${idx + 1}" term="${term}"/>`));
  lines.push('  </extension>');
  lines.push('</archive>');
  return `${lines.join('\n')}\n`;
}

function xmlEscape(text) {
  return String(text)
    .replaceAll('&', '&amp;')
    .replaceAll('<', '&lt;')
    .replaceAll('>', '&gt;')
    .replaceAll('"', '&quot;')
    .replaceAll("'", '&apos;');
}

function loadEmlOverrides(filePath) {
  const target = clean(filePath);
  if (!target) return {};
  const resolved = path.resolve(target);
  if (!fs.existsSync(resolved)) {
    throw new Error(`EML overrides file does not exist: ${resolved}`);
  }
  const obj = JSON.parse(fs.readFileSync(resolved, 'utf8'));
  if (typeof obj !== 'object' || obj === null || Array.isArray(obj)) {
    throw new Error('EML overrides JSON must be an object.');
  }
  return obj;
}

function buildEmlXml(project, overrides, projectId) {
  const packageId = clean(overrides.package_id) || '8b8c78fb-53d7-434c-b800-61f38b30d0c9/v62.434';
  const alternateId1 = clean(overrides.alternate_identifier_1) || '8b8c78fb-53d7-434c-b800-61f38b30d0c9';
  const alternateId2 = clean(overrides.alternate_identifier_2) || clean(overrides.alternate_identifier)
    || 'https://bnhmipt.berkeley.edu/resource?r=biocode';
  const datasetTitle = clean(overrides.dataset_title) || 'Moorea Biocode Project';
  const abstractText = clean(overrides.abstract)
    || 'The Moorea Biocode project has the ambitious goal of DNA barcoding an entire ecosystem: the island of Moorea, located in French Polynesia. This ecosystem has over 5,000 identified species and the project itself has collected and sequenced over 30,000 specimens. As part of the collecting effort in the Moorea Biocode project, we have developed informatics tools to track data from the collecting event, specimen identification, photograph, laboratory, and ultimately to host institution and sequence repositories.';
  const acknowledgements = clean(overrides.acknowledgements) || 'Funding from the Gordon and Betty Moore Foundation';
  const pubDate = clean(overrides.pub_date) || '2026-02-19';
  const dateStamp = clean(overrides.gbif_date_stamp) || '2016-06-20T04:24:50.335-07:00';
  const replaces = clean(overrides.gbif_replaces) || '8b8c78fb-53d7-434c-b800-61f38b30d0c9/v62.434.xml';
  const projectTitle = clean(overrides.project_title) || 'Moorea Biocode Project';
  const methodsText = clean(overrides.methods) || '';

  return `<eml:eml xmlns:eml="https://eml.ecoinformatics.org/eml-2.2.0"
         xmlns:dc="http://purl.org/dc/terms/"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="https://eml.ecoinformatics.org/eml-2.2.0 https://rs.gbif.org/schema/eml-gbif-profile/1.3/eml.xsd"
         packageId="${xmlEscape(packageId)}" system="http://gbif.org" scope="system"
         xml:lang="eng">
    <!-- LEGACY PLACEHOLDER TEMPLATE:
         This EML content is copied from the previous Moorea Biocode archive output.
         TODO: map these values from GEOME project metadata fields and remove placeholders/comments. -->
    <dataset>
        <alternateIdentifier>${xmlEscape(alternateId1)}</alternateIdentifier>
        <alternateIdentifier>${xmlEscape(alternateId2)}</alternateIdentifier>
        <title xml:lang="eng">${xmlEscape(datasetTitle)}</title>
        <!-- TODO(GEOME metadata mapping): creator and metadataProvider -->
        <creator>
            <individualName>
                <givenName>Chris</givenName>
                <surName>Meyer</surName>
            </individualName>
            <organizationName>Smithsonian Institution National Museum of Natural History</organizationName>
            <positionName>Programmer</positionName>
            <address>
                <deliveryPoint>1096 Valley Life Sciences Building</deliveryPoint>
            </address>
            <electronicMailAddress>jdeck@berkeley.edu</electronicMailAddress>
            <electronicMailAddress>meyerc@si.edu</electronicMailAddress>
        </creator>
        <metadataProvider>
            <individualName>
                <givenName>Chris</givenName>
                <surName>Meyer</surName>
            </individualName>
            <organizationName>Smithsonian Institution National Museum of Natural History</organizationName>
            <positionName>Collections Manager</positionName>
            <address>
                <country>US</country>
            </address>
            <electronicMailAddress>meyerc@si.edu</electronicMailAddress>
        </metadataProvider>
        <!-- TODO(GEOME metadata mapping): associated parties -->
        <associatedParty>
            <individualName>
                <givenName>Neil</givenName>
                <surName>Davies</surName>
            </individualName>
            <organizationName>Gump South Pacific Research Station</organizationName>
            <role>originator</role>
        </associatedParty>
        <associatedParty>
            <individualName>
                <givenName>John</givenName>
                <surName>Deck</surName>
            </individualName>
            <organizationName>Berkeley Natural History Museums</organizationName>
            <role>programmer</role>
        </associatedParty>
        <associatedParty>
            <individualName>
                <givenName>George</givenName>
                <surName>Roderick</surName>
            </individualName>
            <organizationName>University of California, Berkeley</organizationName>
            <role>originator</role>
        </associatedParty>
        <associatedParty>
            <individualName>
                <givenName>Serge</givenName>
                <surName>PLANES</surName>
            </individualName>
            <organizationName>Centre National de la Recherche Scientifique</organizationName>
            <role>originator</role>
        </associatedParty>
        <associatedParty>
            <individualName>
                <givenName>Jean-Yves</givenName>
                <surName>Meyer</surName>
            </individualName>
            <organizationName>State Secretariat for Education, Research and Innovation</organizationName>
            <role>originator</role>
        </associatedParty>
        <associatedParty>
            <individualName>
                <givenName>Claude</givenName>
                <surName>PAYRI</surName>
            </individualName>
            <organizationName>University of French Polynesia</organizationName>
            <role>originator</role>
        </associatedParty>
        <associatedParty>
            <individualName>
                <givenName>Gustav</givenName>
                <surName>Paulay</surName>
            </individualName>
            <organizationName>University of Florida</organizationName>
            <role>originator</role>
        </associatedParty>
        <associatedParty>
            <individualName>
                <givenName>Richard</givenName>
                <surName>Pyle</surName>
            </individualName>
            <organizationName>Bernice P. Bishop Museum</organizationName>
            <role>originator</role>
        </associatedParty>
        <associatedParty>
            <individualName>
                <givenName>Sylvain</givenName>
                <surName>Charlat</surName>
            </individualName>
            <organizationName>Université de Lyon</organizationName>
            <role>originator</role>
        </associatedParty>
        <associatedParty>
            <individualName>
                <givenName>Craig</givenName>
                <surName>Moritz</surName>
            </individualName>
            <organizationName>University of California, Berkeley</organizationName>
            <role>originator</role>
        </associatedParty>
        <associatedParty>
            <individualName>
                <givenName>Matteo</givenName>
                <surName>Garbelotto</surName>
            </individualName>
            <organizationName>University of California, Berkeley</organizationName>
            <role>originator</role>
        </associatedParty>
        <associatedParty>
            <individualName>
                <givenName>Olivier</givenName>
                <surName>Gargominy</surName>
            </individualName>
            <organizationName>National Museum of Natural History</organizationName>
            <role>originator</role>
        </associatedParty>
        <associatedParty>
            <individualName>
                <givenName>Brent</givenName>
                <surName>Mishler</surName>
            </individualName>
            <organizationName>University of California, Berkeley</organizationName>
            <role>originator</role>
        </associatedParty>
        <pubDate>${xmlEscape(pubDate)}</pubDate>
        <language>eng</language>
        <abstract>
            <para>${xmlEscape(abstractText)}</para>
        </abstract>
        <!-- TODO(GEOME metadata mapping): keyword sets -->
        <keywordSet>
            <keyword>Occurrence</keyword>
            <keywordThesaurus>GBIF Dataset Type Vocabulary: http://rs.gbif.org/vocabulary/gbif/dataset_type.xml</keywordThesaurus>
        </keywordSet>
        <keywordSet>
            <keyword>Specimen</keyword>
            <keywordThesaurus>GBIF Dataset Subtype Vocabulary: http://rs.gbif.org/vocabulary/gbif/dataset_subtype.xml</keywordThesaurus>
        </keywordSet>
        <keywordSet>
            <keyword>Occurrence</keyword>
            <keywordThesaurus>GBIF Dataset Type Vocabulary: http://rs.gbif.org/vocabulary/gbif/dataset_type_2015-07-10.xml</keywordThesaurus>
        </keywordSet>
        <additionalInfo>
            <para>Please see http://vertnet.org/resources/norms.html for additional usage information.</para>
        </additionalInfo>
        <!-- TODO(GEOME metadata mapping): rights/license statement -->
        <intellectualRights>
            <para>This work is licensed under a <ulink url="http://creativecommons.org/licenses/by/4.0/legalcode"><citetitle>Creative Commons Attribution (CC-BY) 4.0 License</citetitle></ulink>.</para>
        </intellectualRights>
        <!-- TODO(GEOME metadata mapping): distribution URLs -->
        <distribution scope="document">
            <online>
                <url function="information">http://biocode.berkeley.edu</url>
            </online>
        </distribution>
        <distribution scope="document">
            <online>
                <url function="download">https://bnhmipt.berkeley.edu/archive.do?r=biocode</url>
            </online>
        </distribution>
        <!-- TODO(GEOME metadata mapping): geographic coverage -->
        <coverage>
            <geographicCoverage>
                <geographicDescription>Moorea, French Polynesia</geographicDescription>
                <boundingCoordinates>
                    <westBoundingCoordinate>-152.512</westBoundingCoordinate>
                    <eastBoundingCoordinate>-148.052</eastBoundingCoordinate>
                    <northBoundingCoordinate>-15.665</northBoundingCoordinate>
                    <southBoundingCoordinate>-18.792</southBoundingCoordinate>
                </boundingCoordinates>
            </geographicCoverage>
        </coverage>
        <acknowledgements>${xmlEscape(acknowledgements)}</acknowledgements>
        <maintenance>
            <description>
                <para></para>
            </description>
            <maintenanceUpdateFrequency>weekly</maintenanceUpdateFrequency>
        </maintenance>
        <contact>
            <individualName>
                <givenName>Chris</givenName>
                <surName>Meyer</surName>
            </individualName>
            <organizationName>Smithsonian Institution National Museum of Natural History</organizationName>
            <electronicMailAddress>meyerc@si.edu</electronicMailAddress>
        </contact>
        <methods>
            <methodStep>
                <description>
                    <para>${xmlEscape(methodsText)}</para>
                </description>
            </methodStep>
        </methods>
        <!-- TODO(GEOME metadata mapping): project personnel -->
        <project>
            <title>${xmlEscape(projectTitle)}</title>
            <personnel>
                <individualName>
                    <givenName>John</givenName>
                    <surName>Deck</surName>
                </individualName>
                <userId directory="http://orcid.org/">0000-0002-5905-1617</userId>
                <role></role>
            </personnel>
        </project>
    </dataset>
    <additionalMetadata>
        <metadata>
            <gbif>
                <dateStamp>${xmlEscape(dateStamp)}</dateStamp>
                <hierarchyLevel>dataset</hierarchyLevel>
                <dc:replaces>${xmlEscape(replaces)}</dc:replaces>
            </gbif>
        </metadata>
    </additionalMetadata>
</eml:eml>
`;
}

function postForm(url, formData, timeoutMs = 60000) {
  return new Promise((resolve, reject) => {
    const parsed = new URL(url);
    const body = new URLSearchParams(formData).toString();
    const lib = parsed.protocol === 'https:' ? https : http;

    const req = lib.request({
      protocol: parsed.protocol,
      hostname: parsed.hostname,
      port: parsed.port,
      path: `${parsed.pathname}${parsed.search}`,
      method: 'POST',
      headers: {
        'Content-Type': 'application/x-www-form-urlencoded',
        'Content-Length': Buffer.byteLength(body)
      },
      timeout: timeoutMs
    }, (res) => {
      const chunks = [];
      res.on('data', (d) => chunks.push(d));
      res.on('end', () => {
        const text = Buffer.concat(chunks).toString('utf8');
        resolve({ statusCode: res.statusCode || 0, body: text });
      });
    });

    req.on('timeout', () => {
      req.destroy(new Error('HTTP timeout')); 
    });
    req.on('error', reject);
    req.write(body);
    req.end();
  });
}

async function authToken(apiBase, username, password, clientId, clientSecret) {
  const url = `${apiBase.replace(/\/$/, '')}/oauth/accessToken`;
  const response = await postForm(url, {
    grant_type: 'password',
    username,
    password,
    client_id: clientId,
    client_secret: clientSecret
  });

  if (response.statusCode >= 400) {
    throw new Error(`OAuth authentication failed (${response.statusCode}): ${response.body.slice(0, 500)}`);
  }

  let payload;
  try {
    payload = JSON.parse(response.body);
  } catch (err) {
    throw new Error(`OAuth response is not JSON: ${response.body.slice(0, 500)}`);
  }

  const token = clean(payload.access_token);
  if (!token) {
    throw new Error(`OAuth response missing access_token: ${response.body.slice(0, 500)}`);
  }

  return token;
}

function validateSchema(schema) {
  if (!/^[A-Za-z_][A-Za-z0-9_]*$/.test(schema)) {
    throw new Error(`Invalid schema name: ${schema}`);
  }
  return schema;
}

function buildPgConfig(env) {
  if (clean(env.DB_DSN)) {
    return { connectionString: clean(env.DB_DSN) };
  }

  const config = {
    host: required(env, 'DB_HOST'),
    port: Number(clean(env.DB_PORT) || '5432'),
    database: required(env, 'DB_NAME'),
    user: required(env, 'DB_USER'),
    password: required(env, 'DB_PASSWORD')
  };

  const sslMode = clean(env.DB_SSLMODE).toLowerCase();
  if (sslMode && sslMode !== 'disable') {
    config.ssl = { rejectUnauthorized: sslMode === 'verify-full' };
  }

  return config;
}

async function getProjectRow(client, projectId) {
  const { rows } = await client.query(`
    SELECT p.id,
           p.project_title,
           p.localcontexts_id,
           p.permit_guid,
           p.license,
           p.user_id,
           u.first_name,
           u.last_name,
           u.email,
           u.institution
    FROM projects p
    LEFT JOIN users u ON p.user_id = u.id
    WHERE p.id = $1
  `, [projectId]);
  if (!rows.length) throw new Error(`Project ${projectId} not found.`);
  return rows[0];
}

async function getSampleRows(client, projectId, schema) {
  const sql = `
    SELECT e.expedition_code,
           s.expedition_id,
           s.local_identifier AS sample_id,
           s.parent_identifier AS parent_event_id,
           s.data AS sample_data,
           ev.data AS event_data,
           sample_identifier.identifier AS sample_prefix
    FROM expeditions e
    JOIN ${schema}.sample s ON s.expedition_id = e.id
    LEFT JOIN ${schema}.event ev
        ON ev.expedition_id = s.expedition_id
       AND ev.local_identifier = s.parent_identifier
    LEFT JOIN entity_identifiers sample_identifier
        ON sample_identifier.expedition_id = e.id
       AND sample_identifier.concept_alias = 'Sample'
    WHERE e.project_id = $1
    ORDER BY e.expedition_code, s.local_identifier, s.expedition_id
  `;
  const { rows } = await client.query(sql, [projectId]);
  return rows;
}

async function getPhotoRows(client, projectId, schema) {
  const sql = `
    SELECT sp.parent_identifier AS sample_id,
           sp.data AS photo_data,
           photo_identifier.identifier AS photo_prefix
    FROM expeditions e
    JOIN ${schema}.sample_photo sp ON sp.expedition_id = e.id
    LEFT JOIN entity_identifiers photo_identifier
        ON photo_identifier.expedition_id = e.id
       AND photo_identifier.concept_alias = 'Sample_Photo'
    WHERE e.project_id = $1
    ORDER BY sp.parent_identifier, COALESCE(sp.data->>'photoID', '')
  `;
  const { rows } = await client.query(sql, [projectId]);
  return rows;
}

function buildSampleBcid(samplePrefix, sampleId, sampleData) {
  const prefix = clean(samplePrefix);
  const sid = clean(sampleId);
  if (prefix && sid) return `${prefix}${sid}`;
  const catalog = firstNonEmpty(sampleData, ['urn:catalogNumber', 'catalogNumber', 'urn:materialSampleID', 'materialSampleID']);
  return catalog;
}

function rowForOccurrence(project, row, dynamicProps) {
  const sampleData = getJsonObject(row.sample_data);
  const eventData = getJsonObject(row.event_data);

  const sampleId = clean(row.sample_id);
  const sampleBcid = buildSampleBcid(row.sample_prefix, sampleId, sampleData);

  const record = Object.fromEntries(OCCURRENCE_HEADERS.map((h) => [h, '']));
  record.id = sampleId;
  record.references = toN2t(sampleBcid);
  record.dynamicProperties = dynamicProps;
  record.eventID = clean(row.parent_event_id) || firstNonEmpty(eventData, EVENT_ALIASES.eventID);

  [
    'rightsHolder', 'accessRights', 'institutionCode', 'basisOfRecord', 'occurrenceID', 'catalogNumber',
    'occurrenceRemarks', 'individualCount', 'sex', 'lifeStage', 'otherCatalogNumbers',
    'previousIdentifications', 'associatedTaxa', 'identifiedBy', 'identificationRemarks',
    'identificationVerificationStatus', 'typeStatus', 'scientificName', 'kingdom', 'phylum', 'class',
    'order', 'family', 'genus', 'subgenus', 'specificEpithet', 'infraspecificEpithet', 'taxonRank', 'taxonRemarks'
  ].forEach((field) => {
    record[field] = firstNonEmpty(sampleData, SAMPLE_ALIASES[field]);
  });

  [
    'samplingProtocol', 'year', 'month', 'day', 'habitat', 'eventRemarks', 'islandGroup', 'island',
    'country', 'stateProvince', 'county', 'locality', 'minimumElevationInMeters',
    'maximumElevationInMeters', 'minimumDepthInMeters', 'maximumDepthInMeters',
    'verbatimLatitude', 'verbatimLongitude', 'decimalLatitude', 'decimalLongitude',
    'coordinateUncertaintyInMeters'
  ].forEach((field) => {
    record[field] = firstNonEmpty(eventData, EVENT_ALIASES[field]);
  });

  if (!record.occurrenceID) record.occurrenceID = sampleId;
  if (!record.catalogNumber) record.catalogNumber = sampleId;
  if (!record.accessRights) record.accessRights = clean(project.license);
  if (!record.basisOfRecord) record.basisOfRecord = 'MaterialSample';

  record.decimalLatitude = formatDecimal(record.decimalLatitude);
  record.decimalLongitude = formatDecimal(record.decimalLongitude);

  return record;
}

function rowForMultimedia(row) {
  const data = getJsonObject(row.photo_data);
  const sampleId = clean(row.sample_id);
  const photoId = firstNonEmpty(data, ['photoID', 'urn:photoID']);
  const photoPrefix = clean(row.photo_prefix);

  const uri = firstNonEmpty(data, ['img1024', 'img512', 'img128', 'identifier', 'urn:identifier', 'accessURI', 'urn:accessURI']);
  if (!uri) return null;

  const media = Object.fromEntries(MULTIMEDIA_HEADERS.map((h) => [h, '']));
  media.id = sampleId;
  media.identifier = uri;
  media.type = 'StillImage';
  media.Owner = firstNonEmpty(data, ['urn:samplePhotographer', 'samplePhotographer', 'owner', 'Owner']);
  media.accessURI = uri;
  media.format = firstNonEmpty(data, ['format', 'urn:format']) || inferFormat(uri);

  if (photoPrefix && photoId) {
    media.furtherInformationURL = toN2t(`${photoPrefix}${photoId}`);
  }

  return media;
}

function tsvCell(value) {
  return clean(value).replace(/[\t\r\n]/g, ' ');
}

function writeTsv(filePath, headers, rows) {
  const lines = [headers.join('\t')];
  for (const row of rows) {
    lines.push(headers.map((h) => tsvCell(row[h])).join('\t'));
  }
  fs.writeFileSync(filePath, `${lines.join('\n')}\n`, 'utf8');
}

function zipDwca(outputZip, files) {
  if (fs.existsSync(outputZip)) {
    fs.unlinkSync(outputZip);
  }
  const args = ['-q', '-j', outputZip, ...files];
  const result = spawnSync('zip', args, { encoding: 'utf8' });
  if (result.error) {
    throw new Error(`Failed to run zip command: ${result.error.message}`);
  }
  if (result.status !== 0) {
    throw new Error(`zip command failed (${result.status}): ${result.stderr || result.stdout}`);
  }
}

function asBool(value, defaultValue = false) {
  const text = clean(value).toLowerCase();
  if (!text) return defaultValue;
  return ['1', 'true', 'yes', 'y', 'on'].includes(text);
}

function parseArgs(argv) {
  const args = {
    envFile: '.env',
    dbPropsFile: 'src/main/environment/local/biocode-fims-database.properties',
    projectId: null,
    networkSchema: null,
    emlOverridesJson: null,
    verbose: false
  };

  for (let i = 2; i < argv.length; i += 1) {
    const key = argv[i];
    if (key === '--help' || key === '-h') {
      printHelp();
      process.exit(0);
    }
    if (key === '--verbose') {
      args.verbose = true;
      continue;
    }
    const next = argv[i + 1];
    if (!next || next.startsWith('--')) throw new Error(`Missing value for ${key}`);

    switch (key) {
      case '--env-file':
        args.envFile = next;
        break;
      case '--db-props-file':
        args.dbPropsFile = next;
        break;
      case '--project-id':
        args.projectId = Number(next);
        if (Number.isNaN(args.projectId)) throw new Error(`Invalid --project-id: ${next}`);
        break;
      case '--network-schema':
        args.networkSchema = next;
        break;
      case '--eml-overrides-json':
        args.emlOverridesJson = next;
        break;
      default:
        throw new Error(`Unknown argument: ${key}`);
    }
    i += 1;
  }

  return args;
}

function printHelp() {
  console.log(`Usage: node scripts/generate_dwca_biocode.js [options]

Options:
  --env-file <path>             Path to .env file (default: .env)
  --db-props-file <path>        Java DB props fallback file
                                (default: src/main/environment/local/biocode-fims-database.properties)
  --project-id <id>             GEOME project id (default: DWCA_PROJECT_ID or 75)
  --network-schema <schema>     DB schema (default: GEOME_NETWORK_SCHEMA or network_1)
  --eml-overrides-json <path>   Optional EML override JSON file
  --verbose                     Print progress
  -h, --help                    Show this help
`);
}

async function main() {
  const args = parseArgs(process.argv);
  ensureDependencies();

  const env = mergedEnv(path.resolve(args.envFile), path.resolve(args.dbPropsFile));
  const projectId = args.projectId || Number(clean(env.DWCA_PROJECT_ID) || '75');
  const apiBase = clean(env.GEOME_API_BASE) || 'https://api.geome-db.org';
  const networkSchema = validateSchema(args.networkSchema || clean(env.GEOME_NETWORK_SCHEMA) || 'network_1');

  const outputDir = path.resolve(clean(env.DWCA_OUTPUT_DIR) || 'output/dwca');
  const outputBase = clean(env.DWCA_OUTPUT_BASENAME) || `dwca-project-${projectId}`;
  const appendTimestamp = asBool(env.DWCA_APPEND_TIMESTAMP, false);
  fs.mkdirSync(outputDir, { recursive: true });

  if (args.verbose) {
    console.log(`Authenticating against ${apiBase} as ${clean(env.GEOME_USERNAME)}...`);
  }

  const token = await authToken(
    apiBase,
    required(env, 'GEOME_USERNAME'),
    required(env, 'GEOME_PASSWORD'),
    required(env, 'GEOME_CLIENT_ID'),
    required(env, 'GEOME_CLIENT_SECRET')
  );

  if (args.verbose) {
    console.log(`OAuth token acquired (length=${token.length}). Querying project ${projectId}...`);
  }

  const emlOverrides = loadEmlOverrides(args.emlOverridesJson || clean(env.DWCA_EML_OVERRIDES_JSON));

  const client = new PgClient(buildPgConfig(env));
  await client.connect();

  let project;
  let sampleRows;
  let photoRows;
  try {
    project = await getProjectRow(client, projectId);
    sampleRows = await getSampleRows(client, projectId, networkSchema);
    photoRows = await getPhotoRows(client, projectId, networkSchema);
  } finally {
    await client.end();
  }

  const props = dynamicProperties(project);
  const occurrences = sampleRows.map((row) => rowForOccurrence(project, row, props));
  const media = photoRows.map((row) => rowForMultimedia(row)).filter(Boolean);

  occurrences.sort((a, b) => `${clean(a.id)}|${clean(a.eventID)}`.localeCompare(`${clean(b.id)}|${clean(b.eventID)}`));
  media.sort((a, b) => `${clean(a.id)}|${clean(a.identifier)}`.localeCompare(`${clean(b.id)}|${clean(b.identifier)}`));

  let bundleName = outputBase;
  if (appendTimestamp) {
    const stamp = new Date().toISOString().replace(/[-:]/g, '').replace(/\.\d+Z$/, 'Z');
    bundleName = `${outputBase}-${stamp}`;
  }

  const stagingDir = fs.mkdtempSync(path.join(outputDir, `${bundleName}-`));

  const occurrencePath = path.join(stagingDir, 'occurrence.txt');
  const multimediaPath = path.join(stagingDir, 'multimedia.txt');
  const metaPath = path.join(stagingDir, 'meta.xml');
  const emlPath = path.join(stagingDir, 'eml.xml');

  writeTsv(occurrencePath, OCCURRENCE_HEADERS, occurrences);
  writeTsv(multimediaPath, MULTIMEDIA_HEADERS, media);
  fs.writeFileSync(metaPath, buildMetaXml(), 'utf8');
  fs.writeFileSync(emlPath, buildEmlXml(project, emlOverrides, projectId), 'utf8');

  const zipPath = path.join(outputDir, `${bundleName}.zip`);
  zipDwca(zipPath, [occurrencePath, multimediaPath, metaPath, emlPath]);

  console.log(`DWCA created: ${zipPath}`);
  console.log(`Staging files: ${stagingDir}`);
  console.log(`Occurrence rows: ${occurrences.length}`);
  console.log(`Multimedia rows: ${media.length}`);
}

main().catch((err) => {
  console.error(err.message || err);
  process.exit(1);
});
