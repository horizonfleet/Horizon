import React, { Component } from "react";
import Detailheader from "./Detailheader";
import Detailrow from "./Detailrow";

class Detail extends Component {
  state = {};
  render() {
    return (
      <div className="Detail">
        <Detailheader />
        <div className="Detail-List">
          <Detailrow />
          <Detailrow />
          <Detailrow />
          <Detailrow />
          <Detailrow />
          <Detailrow />
        </div>
      </div>
    );
  }
}

export default Detail;
