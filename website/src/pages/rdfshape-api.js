import React from 'react'
import {Redirect} from '@docusaurus/router'
import useBaseUrl from '@docusaurus/useBaseUrl'

// Redirect to home page
const RdfShapeApi = () => {
    const redirectHome = useBaseUrl("/")
    return <Redirect to={redirectHome}/>
}

export default RdfShapeApi