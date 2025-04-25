import {
    Autocomplete,
    Button,
    Chip,
    Dialog,
    DialogActions,
    DialogContent,
    DialogTitle,
    FormControl,
    InputLabel,
    MenuItem,
    Select,
    TextField
} from "@mui/material";
import React from "react";
import {
    ALL_STATUSES,
    ALL_TYPES,
    Environment, ENVIRONMENT_TYPES_MAPPING,
    EnvironmentStatus,
    EnvironmentType,
    STATUS_MAPPING
} from "../entities/environments";

type Props = {
    environment: Environment;
    allLabels: string[];
    onClose: () => void;
    onSave: (env: Environment) => void;
};

export default function EditEnvironmentDialog({environment, allLabels, onClose, onSave}: Props) {

    const [localEnv, setLocalEnv] = React.useState<Environment>(environment);
    const handleSubmit = () => {
        onSave(localEnv);
    };

    return <Dialog open={!!localEnv} onClose={onClose} fullWidth={true} maxWidth="sm">
        <DialogTitle>Edit Environment</DialogTitle>
        <DialogContent>
            <TextField
                label="Name"
                value={localEnv.name || ''}
                disabled
                fullWidth
                margin="dense"/>
            <TextField
                label="Owner"
                value={localEnv.owner || ''}
                onChange={e => setLocalEnv(prevState => ({...prevState, owner: e.target.value}))}
                fullWidth
                margin="dense"
            />
            <FormControl sx={{mt: 1, mb: 1}} fullWidth>
                <InputLabel>Status</InputLabel>
                <Select
                    value={localEnv.status || ''}
                    onChange={e => setLocalEnv(prev => ({...prev, status: e.target.value as EnvironmentStatus}))}
                    fullWidth
                    label="Status"
                    margin="dense"
                >
                    {ALL_STATUSES.map(status => <MenuItem key={status}
                                                          value={status}>{STATUS_MAPPING[status]}</MenuItem>)}
                </Select>
            </FormControl>
            <FormControl sx={{mt: 1, mb: 1}} fullWidth>
                <InputLabel>Type</InputLabel>
                <Select
                    value={localEnv.type || ''}
                    onChange={e => setLocalEnv(prev => ({...prev, type: e.target.value as EnvironmentType}))}
                    fullWidth
                    label="Environment Type"
                    margin="dense"
                >
                    {ALL_TYPES.map(type => <MenuItem key={type}
                                                          value={type}>{ENVIRONMENT_TYPES_MAPPING[type]}</MenuItem>)}
                </Select>
            </FormControl>
            <TextField
                label="Description"
                value={localEnv.description || ''}
                onChange={e => setLocalEnv(prev => ({...prev, description: e.target.value}))}
                fullWidth
                margin="dense"
            />
            <FormControl sx={{mt: 1, mb: 1}} fullWidth>
                <Autocomplete
                    multiple
                    options={allLabels}
                    defaultValue={localEnv.labels}
                    freeSolo
                    renderValue={(value: readonly string[], getItemProps) =>
                        value.map((option: string, index: number) => {
                            const {key, ...itemProps} = getItemProps({index});
                            return (
                                <Chip label={option} key={key} {...itemProps} />
                            );
                        })
                    }
                    renderInput={(params) => (
                        <TextField
                            {...params}
                            label="Labels"
                            placeholder="Labels"
                        />
                    )}
                    onChange={(event, value) => {
                        setLocalEnv(prev => ({...prev, labels: value}));
                    }}
                />
            </FormControl>

        </DialogContent>
        <DialogActions>
            <Button onClick={onClose} color="secondary">Close</Button>
            <Button onClick={handleSubmit} color="primary">Save Changes</Button>
        </DialogActions>
    </Dialog>
}
