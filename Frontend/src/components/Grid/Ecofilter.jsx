import React from "react";
import { makeStyles } from "@material-ui/core/styles";
import FormLabel from "@material-ui/core/FormLabel";
import FormControl from "@material-ui/core/FormControl";
import FormGroup from "@material-ui/core/FormGroup";
import FormControlLabel from "@material-ui/core/FormControlLabel";
import FormHelperText from "@material-ui/core/FormHelperText";
import Checkbox from "@material-ui/core/Checkbox";
import { useEffect } from "react";

const useStyles = makeStyles((theme) => ({
  root: {
    display: "flex",
  },
  formControl: {
    margin: theme.spacing(3),
  },
}));

export default function CheckboxesGroup(props) {
  const classes = useStyles();
  console.log(props.state);
  const [state, setState] = React.useState({
    eco1: props.state.eco1,
    eco2: props.state.eco2,
    eco3: props.state.eco3,
    warn1: props.state.warn1,
  });

  const handleChange = (event) => {
    setState({ ...state, [event.target.name]: event.target.checked });
    props.handleEcoChecks(event);
  };

  const { eco1, eco2, eco3, warn1 } = state;
  //   const error = [eco1, eco2, eco3].filter((v) => v).length !== 2;

  return (
    <div className={classes.root}>
      <FormControl component="fieldset" className={classes.formControl}>
        <FormLabel component="legend">Filter Eco Information</FormLabel>
        <FormGroup>
          <FormControlLabel
            control={
              <Checkbox
                color="primary"
                checked={eco1}
                onChange={handleChange}
                name="eco1"
              />
            }
            label="eco1"
          />
          <FormControlLabel
            control={
              <Checkbox
                color="primary"
                checked={eco2}
                onChange={handleChange}
                name="eco2"
              />
            }
            label="eco2"
          />
          <FormControlLabel
            control={
              <Checkbox
                color="primary"
                checked={eco3}
                onChange={handleChange}
                name="eco3"
              />
            }
            label="eco3"
          />
          <FormControlLabel
            control={
              <Checkbox
                color="primary"
                checked={warn1}
                onChange={handleChange}
                name="warn1"
              />
            }
            label="warn1"
          />
        </FormGroup>
      </FormControl>
    </div>
  );
}
