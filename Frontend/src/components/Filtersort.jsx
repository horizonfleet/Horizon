import React, { Component } from "react";
import {
  FaSortAmountDownAlt,
  FaSortAmountDown,
  FaSortAmountUpAlt,
} from "react-icons/fa";

class Filtersort extends Component {
  render() {
    return (
      //   <form onSubmit={this.handleSubmit}>
      <React.Fragment>
        <select
          className="detail-sort-dropdown"
          value={this.props.column}
          onChange={(event) => this.props.handleChange(event.target.value)}
        >
          <option value="data_id">Id</option>
          <option value="truck_speed">Speed</option>
        </select>
        {this.props.sortDirection === "asc" ? (
          <FaSortAmountDownAlt
            style={{ paddingLeft: "10px" }}
            onClick={() =>
              this.props.setSortDirection(this.props.sortDirection)
            }
          />
        ) : (
          <FaSortAmountUpAlt
            style={{ paddingLeft: "10px" }}
            onClick={() =>
              this.props.setSortDirection(this.props.sortDirection)
            }
          />
        )}
        {/* <button
          onClick={() => this.props.setSortDirection(this.props.sortDirection)}
        >
          {this.props.sortDirection}
        </button> */}
      </React.Fragment>
    );
  }
}

export default Filtersort;
