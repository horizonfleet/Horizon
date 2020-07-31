import React, { Component } from "react";
import { FaExpandAlt } from "react-icons/fa";

class Detailheader extends Component {
  state = {};
  render() {
    return (
      <div className="Header">
        <div class="flex-container">
          <div>ID</div>
          <div>TRUCK</div>
          <div>GROUP</div>
          <div>VEHICLE STATUS</div>
          <div>DEPARTURE</div>
          <div>ARRIVAL</div>
          <div>ECODRIVING</div>
          <div>WARNINGS</div>
          <FaExpandAlt className="Toggle-Details" />
        </div>
      </div>
    );
  }
}

export default Detailheader;
