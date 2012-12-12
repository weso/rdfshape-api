SELECT 
  t.transLabel
FROM 
 translation t
 INNER JOIN language l ON l.id = t.langid
 INNER JOIN iri i ON i.id = t.iriid
WHERE (l.langCode = 'en' and i.iriName='http://xmlns.com/foaf/0.1/Project' )